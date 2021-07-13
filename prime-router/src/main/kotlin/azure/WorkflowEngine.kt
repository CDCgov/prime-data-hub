package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.azure.db.Tables
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.RedoxSerializer
import gov.cdc.prime.router.transport.AS2Transport
import gov.cdc.prime.router.transport.BlobStoreTransport
import gov.cdc.prime.router.transport.NullTransport
import gov.cdc.prime.router.transport.RedoxTransport
import gov.cdc.prime.router.transport.RetryItems
import gov.cdc.prime.router.transport.RetryToken
import gov.cdc.prime.router.transport.SftpTransport
import org.jooq.Configuration
import org.jooq.Field
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime

/**
 * A top-level object that contains all the helpers and accessors to power the workflow.
 * Workflow objects are heavy-weight and should only be created once per function lifetime.
 *
 * @see gov.cdc.prime.router.Report
 * @see QueueAccess
 * @see DatabaseAccess.Header
 */
class WorkflowEngine(
    // Immutable objects can be shared between every function call
    val metadata: Metadata = WorkflowEngine.metadata,
    val settings: SettingsProvider = WorkflowEngine.settings,
    val hl7Serializer: Hl7Serializer = WorkflowEngine.hl7Serializer,
    val csvSerializer: CsvSerializer = WorkflowEngine.csvSerializer,
    val redoxSerializer: RedoxSerializer = WorkflowEngine.redoxSerializer,
    val translator: Translator = Translator(metadata, settings),
    // New connection for every function
    val db: DatabaseAccess = WorkflowEngine.databaseAccess,
    val blob: BlobAccess = BlobAccess(csvSerializer, hl7Serializer, redoxSerializer),
    val queue: QueueAccess = QueueAccess(),
    val sftpTransport: SftpTransport = SftpTransport(),
    val redoxTransport: RedoxTransport = RedoxTransport(),
    val blobStoreTransport: BlobStoreTransport = BlobStoreTransport(),
    val nullTransport: NullTransport = NullTransport(),
    val as2Transport: AS2Transport = AS2Transport()
) {
    /**
     * Check the connections to Azure Storage and DB
     */
    fun checkConnections() {
        db.checkConnection()
        blob.checkConnection()
    }

    /**
     * Place a report into the workflow
     */
    fun receiveReport(
        validatedRequest: ReportFunction.ValidatedRequest,
        actionHistory: ActionHistory,
        txn: Configuration? = null
    ) {
        if (validatedRequest.report == null) error("Cannot receive a null report")
        val blobInfo = blob.uploadBody(validatedRequest.report)
        try {
            val receiveEvent = ReportEvent(Event.EventAction.RECEIVE, validatedRequest.report.id, null)
            db.insertTask(
                validatedRequest.report, blobInfo.format.toString(), blobInfo.blobUrl, receiveEvent, txn
            )
            // todo bodyURL is no longer needed in report; its in 'blobInfo'
            validatedRequest.report.bodyURL = blobInfo.blobUrl
            actionHistory.trackExternalInputReport(validatedRequest, blobInfo)
        } catch (e: Exception) {
            // Clean up
            blob.deleteBlob(blobInfo.blobUrl)
            throw e
        }
    }

    /**
     * Place a report into the workflow (Note:  I moved queueing the message to after the Action is saved)
     */
    fun dispatchReport(
        nextAction: Event,
        report: Report,
        actionHistory: ActionHistory,
        receiver: Receiver,
        txn: Configuration? = null,
        context: ExecutionContext? = null
    ) {
        val blobInfo = try {
            // formatting errors can occur down in here.
            blob.uploadBody(report)
        } catch (ex: Exception) {
            context?.logger?.warning(
                "Got exception while dispatching to schema ${report.schema.name}" +
                    ", and rcvr ${receiver.fullName}"
            )
            throw ex
        }
        try {
            db.insertTask(report, blobInfo.format.toString(), blobInfo.blobUrl, nextAction, txn)
            // todo remove this; its now tracked in BlobInfo
            report.bodyURL = blobInfo.blobUrl
            actionHistory.trackCreatedReport(nextAction, report, receiver, blobInfo)
        } catch (e: Exception) {
            // Clean up
            blob.deleteBlob(blobInfo.blobUrl)
            throw e
        }
    }

    /**
     * Handle a single report event. Callback returns the next action for the report.
     *
     * @param messageEvent received from the message queue
     * @param updateBlock is wrapped in a DB transaction and called
     */
    fun handleReportEvent(
        messageEvent: ReportEvent,
        context: ExecutionContext? = null,
        updateBlock: (header: Header, retryToken: RetryToken?, txn: Configuration?) -> ReportEvent,
    ) {
        var nextEvent: ReportEvent? = null
        db.transact { txn ->
            val reportId = messageEvent.reportId
            val task = db.fetchAndLockTask(reportId, txn)
            val (organization, receiver) = findOrganizationAndReceiver(task.receiverName, txn)
            val reportFile = db.fetchReportFile(reportId, org = null, txn = txn)
            // todo remove this once things are permanently sane ;)
            ActionHistory.sanityCheckReport(task, reportFile, false)
            val itemLineages = db.fetchItemLineagesForReport(reportId, reportFile.itemCount, txn)
            val header = createHeader(task, reportFile, itemLineages, organization, receiver)
            val currentEventAction = Event.EventAction.parseQueueMessage(header.task.nextAction.literal)
            // Ignore messages that are not consistent with the current header
            if (currentEventAction != messageEvent.eventAction) {
                context?.let {
                    context.logger.warning(
                        "Weirdness for $reportId: queue event = ${messageEvent.eventAction.name}, " +
                            " but task.nextAction = ${currentEventAction.name} "
                    )
                }
                return@transact
            }
            val retryToken = RetryToken.fromJSON(header.task.retryToken?.data())

            nextEvent = updateBlock(header, retryToken, txn)
            context?.let { context.logger.info("Finished updateBlock for $reportId") }
            val retryJson = nextEvent!!.retryToken?.toJSON()
            updateHeader(
                header.task.reportId,
                currentEventAction,
                nextEvent!!.eventAction,
                nextEvent!!.at,
                retryJson,
                txn
            )
        }
        if (nextEvent != null) queue.sendMessage(nextEvent!!) // Avoid race condition by doing after txn completes.
    }

    /**
     * Atomic read and set: sanity checks on the state of the report to be resent plus setting the state.
     */
    fun resendEvent(
        reportId: ReportId,
        receiver: Receiver,
        sendFailedOnly: Boolean,
        isTest: Boolean,
        msgs: MutableList<String>,
    ) {
        // Send immediately.
        val nextEvent = ReportEvent(Event.EventAction.SEND, reportId, at = null)
        db.transact { txn ->
            val task = db.fetchAndLockTask(reportId, txn) // variable is not being used.
            val organization = settings.findOrganization(receiver.organizationName)
                ?: throw Exception("No such organization ${receiver.organizationName}")
            val header = fetchHeader(reportId, organization) // exception if not found
            val reportFile = header.reportFile
            if (reportFile.nextAction != TaskAction.send) {
                throw Exception("Cannot send $reportId. Its next action is ${reportFile.nextAction}")
            }
            if (reportFile.bodyUrl.isEmpty()) {
                throw Exception("Cannot send $reportId.  Its not in the blob store.")
            }
            // The report might be old and the receiver has disappeared for some reason.
            if (receiver.fullName != "${reportFile.receivingOrg}.${reportFile.receivingOrgSvc}") {
                throw Exception("Cannot send $reportId. It is not associated with receiver ${receiver.fullName}")
            }
            if (header.task.nextActionAt != null && header.task.nextActionAt.isAfter(OffsetDateTime.now())) {
                throw Exception(
                    "Cannot send $reportId. Its already scheduled to ${header.task.nextAction}" +
                        " at ${header.task.nextActionAt}"
                )
            }
            val retryItems: RetryItems = if (sendFailedOnly) {
                val itemDispositions = fetchItemDispositions(reportFile)
                // Yet another delightful feature of kotlin: groupingBy
                val counts = itemDispositions.values.groupingBy { it }.eachCount()
                msgs.add(
                    "Of ${reportFile.itemCount} items, " +
                        "${counts[RedoxTransport.ResultStatus.SUCCESS] ?: 0 } were sent successfully, " +
                        "${counts[RedoxTransport.ResultStatus.FAILURE] ?: 0} were attempted but failed, " +
                        "${counts[RedoxTransport.ResultStatus.NEVER_ATTEMPTED] ?: 0} were never attempted."
                )
                itemDispositions.filter { (_, disp) ->
                    disp != RedoxTransport.ResultStatus.SUCCESS
                }.map { (key, _) -> key.toString() }
            } else {
                RetryToken.allItems
            }
            if (retryItems.isEmpty()) {
                msgs.add("All Items in $reportId successfully sent.  Nothing to resend. DONE")
            } else {
                if (retryItems == RetryToken.allItems) {
                    msgs.add("Will resend all (${reportFile.itemCount}) items in $reportId")
                } else {
                    msgs.add(
                        "Will resend these failed/not-sent items in $reportId: ${retryItems.joinToString(",")}"
                    )
                }
                if (!isTest) {
                    updateHeader(
                        reportId,
                        Event.EventAction.RESEND,
                        nextEvent.eventAction,
                        nextEvent.at,
                        RetryToken(1, retryItems).toJSON(),
                        txn
                    )
                    msgs.add("$reportId has been queued to resend immediately to ${receiver.fullName}\n")
                } else {
                    msgs.add("Nothing sent.  This was just a test")
                }
            }
        }
        if (!isTest) queue.sendMessage(nextEvent) // Avoid race condition by doing after txn completes.
    }

    /**
     * Given a batched report, look at its children 'sent' reports, and gather the disposition
     * of every item in the batched report.   If there are no children,
     * this will return a map with all item statuses == NEVER_ATTEMPTED
     */
    fun fetchItemDispositions(reportFile: ReportFile): Map<Int, RedoxTransport.ResultStatus> {
        if (reportFile.nextAction != TaskAction.send) {
            throw Exception("Cannot send ${reportFile.reportId}. Its next action is ${reportFile.nextAction}")
        }
        if (reportFile.bodyFormat != Report.Format.REDOX.name) {
            throw Exception("SendFailed option only applies to REDOX reports.  This report is ${reportFile.bodyFormat}")
        }
        // Track what happened to each item.
        val itemsDispositionMap = mutableMapOf<Int, RedoxTransport.ResultStatus>().apply {
            // Initialize all to assume nothing was every done
            for (i in 0 until reportFile.itemCount) this[i] = RedoxTransport.ResultStatus.NEVER_ATTEMPTED
        }
        val childReportIds = db.fetchChildReports(reportFile.reportId)
        childReportIds.forEach { childId ->
            val lineages = db.fetchItemLineagesForReport(childId, reportFile.itemCount)
            lineages?.forEach { lineage ->
                // Once a success, always a success
                if (itemsDispositionMap[lineage.parentIndex] == RedoxTransport.ResultStatus.SUCCESS)
                    return@forEach
                itemsDispositionMap[lineage.parentIndex] = when {
                    lineage.transportResult.startsWith(RedoxTransport.ResultStatus.FAILURE.name) ->
                        RedoxTransport.ResultStatus.FAILURE
                    lineage.transportResult.startsWith(RedoxTransport.ResultStatus.NOT_SENT.name) ->
                        RedoxTransport.ResultStatus.FAILURE
                    else -> RedoxTransport.ResultStatus.SUCCESS
                }
            }
        }
        return itemsDispositionMap
    }

    /**
     * Handle a receiver specific event. Fetch all pending tasks for the specified receiver and nextAction
     *
     * @param messageEvent that was received
     * @param maxCount of headers to process
     * @param updateBlock called with headers to process
     */
    fun handleReceiverEvent(
        messageEvent: ReceiverEvent,
        maxCount: Int,
        updateBlock: (headers: List<Header>, txn: Configuration?) -> Unit,
    ) {
        db.transact { txn ->
            val tasks = db.fetchAndLockTasks(
                messageEvent.eventAction.toTaskAction(),
                messageEvent.at,
                messageEvent.receiverName,
                maxCount,
                txn
            )
            val ids = tasks.map { it.reportId }
            val reportFiles = ids
                .map { db.fetchReportFile(it, org = null, txn = txn) }
                .map { (it.reportId as ReportId) to it }
                .toMap()
            val (organization, receiver) = findOrganizationAndReceiver(messageEvent.receiverName, txn)
            // todo remove this check
            ActionHistory.sanityCheckReports(tasks, reportFiles, false)
            // todo Note that the sanity check means the !! is safe.
            val headers = tasks.map {
                createHeader(it, reportFiles[it.reportId]!!, null, organization, receiver)
            }

            updateBlock(headers, txn)

            headers.forEach {
                val currentAction = Event.EventAction.parseQueueMessage(it.task.nextAction.literal)
                updateHeader(
                    it.task.reportId,
                    currentAction,
                    Event.EventAction.NONE,
                    nextActionAt = null,
                    retryToken = null,
                    txn
                )
            }
        }
    }

    /**
     * Create a report object from a header including loading the blob data associated with it
     */
    fun createReport(header: Header): Report {
        // todo All of this info is already populated in the Header obj.
        val schema = metadata.findSchema(header.task.schemaName)
            ?: error("Invalid schema in queue: ${header.task.schemaName}")
        val bytes = blob.downloadBlob(header.task.bodyUrl)
        return when (header.task.bodyFormat) {
            // TODO after the CSV internal format is flushed from the system, this code will be safe to remove
            "CSV" -> {
                val result = csvSerializer.readExternal(
                    schema.name,
                    ByteArrayInputStream(bytes),
                    emptyList(),
                    header.receiver
                )
                if (result.report == null || result.errors.isNotEmpty()) {
                    error("Internal Error: Could not read a saved CSV blob: ${header.task.bodyUrl}")
                }
                result.report
            }
            "INTERNAL" -> {
                csvSerializer.readInternal(
                    schema.name,
                    ByteArrayInputStream(bytes),
                    emptyList(),
                    header.receiver,
                    header.reportFile.reportId,
                    true
                )
            }
            else -> error("Unsupported read format")
        }
    }

    /**
     * Create a report object from a header including loading the blob data associated with it
     */
    fun readBody(header: Header): ByteArray {
        return blob.downloadBlob(header.task.bodyUrl)
    }

    fun recordAction(actionHistory: ActionHistory, txn: Configuration? = null) {
        if (txn != null) {
            actionHistory.saveToDb(txn)
        } else {
            db.transact { innerTxn -> actionHistory.saveToDb(innerTxn) }
        }
    }

    private fun findOrganizationAndReceiver(
        fullName: String,
        txn: DataAccessTransaction? = null
    ): Pair<Organization, Receiver> {
        return if (settings is SettingsFacade) {
            val (organization, receiver) = settings.findOrganizationAndReceiver(fullName, txn)
                ?: error("Receiver not found in database: $fullName")
            Pair(organization, receiver)
        } else {
            val (organization, receiver) = settings.findOrganizationAndReceiver(fullName)
                ?: error("Invalid receiver name: $fullName")
            Pair(organization, receiver)
        }
    }

    fun fetchDownloadableReportFiles(
        since: OffsetDateTime?,
        organizationName: String,
    ): List<ReportFile> {
        return db.fetchDownloadableReportFiles(since, organizationName)
    }

    /**
     * The header class provides the information needed to process a task.
     */
    data class Header(
        val task: Task,
        val reportFile: ReportFile,
        val itemLineages: List<ItemLineage>?, // ok to not have item-level lineage
        val organization: Organization?,
        val receiver: Receiver?,
        val schema: Schema?,
        val content: ByteArray?
    )

    private fun createHeader(
        task: Task,
        reportFile: ReportFile,
        itemLineages: List<ItemLineage>?,
        organization: Organization?,
        receiver: Receiver?
    ): Header {
        val schema = if (reportFile.schemaName != null)
            metadata.findSchema(reportFile.schemaName)
        else null

        val content = if (reportFile.bodyUrl != null)
            blob.downloadBlob(reportFile.bodyUrl)
        else null
        return Header(task, reportFile, itemLineages, organization, receiver, schema, content)
    }

    fun fetchHeader(
        reportId: ReportId,
        organization: Organization,
    ): Header {
        val reportFile = db.fetchReportFile(reportId, organization)
        val task = db.fetchTask(reportId)
        val (org2, receiver) = findOrganizationAndReceiver(
            reportFile.receivingOrg + "." + reportFile.receivingOrgSvc
        )
        if (org2.name != organization.name) error("${org2.name} != ${organization.name}: Org Name Mismatch check fail")
        // todo remove this sanity check
        ActionHistory.sanityCheckReport(task, reportFile, false)
        val itemLineages = db.fetchItemLineagesForReport(reportId, reportFile.itemCount)
        return createHeader(task, reportFile, itemLineages, organization, receiver)
    }

    /**
     * Update the header of a report with new values
     */
    private fun updateHeader(
        reportId: ReportId,
        currentEventAction: Event.EventAction,
        nextEventAction: Event.EventAction,
        nextActionAt: OffsetDateTime? = null,
        retryToken: String? = null,
        txn: DataAccessTransaction,
    ) {
        fun finishedField(currentEventAction: Event.EventAction): Field<OffsetDateTime> {
            return when (currentEventAction) {
                Event.EventAction.RECEIVE -> Tables.TASK.TRANSLATED_AT
                Event.EventAction.TRANSLATE -> Tables.TASK.TRANSLATED_AT
                Event.EventAction.REBATCH -> Tables.TASK.TRANSLATED_AT // overwrites prior date
                Event.EventAction.BATCH -> Tables.TASK.BATCHED_AT
                Event.EventAction.RESEND -> Tables.TASK.BATCHED_AT // overwrites prior date
                Event.EventAction.SEND -> Tables.TASK.SENT_AT
                Event.EventAction.WIPE -> Tables.TASK.WIPED_AT

                Event.EventAction.BATCH_ERROR,
                Event.EventAction.SEND_ERROR,
                Event.EventAction.WIPE_ERROR -> Tables.TASK.ERRORED_AT

                Event.EventAction.NONE -> error("Internal Error: NONE currentAction")
            }
        }
        db.updateTask(
            reportId, nextEventAction.toTaskAction(), nextActionAt, retryToken, finishedField(currentEventAction), txn
        )
    }

    companion object {
        /**
         * These are all potentially heavy weight objects that
         * should only be created once.
         */
        val metadata: Metadata by lazy {
            val baseDir = System.getenv("AzureWebJobsScriptRoot") ?: "."
            Metadata("$baseDir/metadata")
        }

        val databaseAccess: DatabaseAccess by lazy {
            DatabaseAccess()
        }

        val settings: SettingsProvider by lazy {
            val baseDir = System.getenv("AzureWebJobsScriptRoot") ?: "."
            val primeEnv = System.getenv("PRIME_ENVIRONMENT")
            val settingsEnabled: String? = System.getenv("FEATURE_FLAG_SETTINGS_ENABLED")
            if (settingsEnabled == null || settingsEnabled.equals("true", ignoreCase = true)) {
                SettingsFacade(metadata, databaseAccess)
            } else {
                val ext = primeEnv?.let { "-$it" } ?: ""
                FileSettings("$baseDir/settings", orgExt = ext)
            }
        }

        val csvSerializer: CsvSerializer by lazy {
            CsvSerializer(metadata)
        }

        val hl7Serializer: Hl7Serializer by lazy {
            Hl7Serializer(metadata)
        }

        val redoxSerializer: RedoxSerializer by lazy {
            RedoxSerializer(metadata)
        }
    }
}