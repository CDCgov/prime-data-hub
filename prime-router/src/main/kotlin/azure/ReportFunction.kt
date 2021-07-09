package gov.cdc.prime.router.azure

import com.fasterxml.jackson.core.JsonFactory
import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import gov.cdc.prime.router.ClientSource
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ResultDetail
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.tokens.DatabaseJtiCache
import gov.cdc.prime.router.tokens.FindReportStreamSecretInVault
import gov.cdc.prime.router.tokens.FindSenderKeyInSettings
import gov.cdc.prime.router.tokens.TokenAuthentication
import org.apache.logging.log4j.kotlin.Logging
import org.postgresql.util.PSQLException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.logging.Level

private const val CLIENT_PARAMETER = "client"
private const val OPTION_PARAMETER = "option"
private const val DEFAULT_PARAMETER = "default"
private const val DEFAULT_SEPARATOR = ":"
private const val ROUTE_TO_PARAMETER = "routeTo"
private const val ROUTE_TO_SEPARATOR = ","

/**
 * Azure Functions with HTTP Trigger.
 * This is basically the "front end" of the Hub. Reports come in here.
 */
class ReportFunction: Logging {
    enum class Options {
        None,
        ValidatePayload,
        CheckConnections,
        SkipSend,
        SkipInvalidItems,
        SendImmediately,
    }

    data class ValidatedRequest(
        val httpStatus: HttpStatus,
        val errors: MutableList<ResultDetail> = mutableListOf<ResultDetail>(),
        val warnings: MutableList<ResultDetail> = mutableListOf<ResultDetail>(),
        val options: Options = Options.None,
        val defaults: Map<String, String> = emptyMap(),
        val routeTo: List<String> = emptyList(),
        val report: Report? = null,
    )

    /**
     * POST a report to the router
     *
     * @see ../../../docs/openapi.yml
     */
    @FunctionName("reports")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.FUNCTION
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        return ingestReport(request, context)
    }

    /**
     * POST a report to the router, using FHIR auth security
     * This one is "/api/report".  The other one is "/api/reports"
     */
    @FunctionName("report")
    @StorageAccount("AzureWebJobsStorage")
    fun report(
        @HttpTrigger(
            name = "reqWithFHIRAuth",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        val workflowEngine = WorkflowEngine()
        val tokenAuthentication = TokenAuthentication(DatabaseJtiCache(workflowEngine.db))
        val senderName = request.headers[CLIENT_PARAMETER]
            ?: request.queryParameters.getOrDefault(CLIENT_PARAMETER, "")
        // todo This code is redundant with validateRequest.  Remove from validateRequest once old endpoint is removed
        if (senderName.isBlank())
            return HttpUtilities.bad(request,"Expected a '$CLIENT_PARAMETER' query parameter")
        val sender = workflowEngine.settings.findSender(senderName)
            ?: return HttpUtilities.bad(request, "'$CLIENT_PARAMETER:$senderName': unknown sender")

        tokenAuthentication.checkAccessToken(request, "${sender.fullName}.report")
            ?: return HttpUtilities.unauthorizedResponse(request)
        return ingestReport(request, context)
    }

    private fun ingestReport(request: HttpRequestMessage<String?>, context: ExecutionContext): HttpResponseMessage {
        val workflowEngine = WorkflowEngine()
        val actionHistory = ActionHistory(TaskAction.receive, context)
        var report: Report? = null
        actionHistory.trackActionParams(request)
        val httpResponseMessage = try {
            val validatedRequest = validateRequest(workflowEngine, request)
            when {
                validatedRequest.options == Options.CheckConnections -> {
                    workflowEngine.checkConnections()
                    HttpUtilities.okResponse(request, createResponseBody(validatedRequest))
                }
                validatedRequest.report == null -> {
                    HttpUtilities.httpResponse(
                        request,
                        createResponseBody(validatedRequest),
                        validatedRequest.httpStatus
                    )
                }
                validatedRequest.options == Options.ValidatePayload -> {
                    HttpUtilities.okResponse(request, createResponseBody(validatedRequest))
                }
                else -> {
                    // Regular happy path workflow is here
                    context.logger.info("Successfully reported: ${validatedRequest.report.id}.")
                    report = validatedRequest.report
                    routeReport(context, workflowEngine, validatedRequest, actionHistory)
                    val responseBody = createResponseBody(validatedRequest, actionHistory)
                    workflowEngine.receiveReport(validatedRequest, actionHistory)
                    HttpUtilities.createdResponse(request, responseBody)
                }
            }
        } catch (e: Exception) {
            context.logger.log(Level.SEVERE, e.message, e)
            actionHistory.trackActionResult("Exception: ${e.message ?: e}")
            HttpUtilities.internalErrorResponse(request)
        }
        actionHistory.trackActionResult(httpResponseMessage)
        workflowEngine.recordAction(actionHistory)
        actionHistory.queueMessages() // Must be done after creating TASK record.
        // write the data to the table if we're dealing with covid-19. this has to happen
        // here AFTER we've written the report to the DB
        writeCovidResultMetadataForReport(report, context, workflowEngine)
        return httpResponseMessage
    }

    /**
     * Given a report object, it collects the non-PII, non-PHI out of it and then saves it to the
     * database in the covid_results_metadata table.
     */
    private fun writeCovidResultMetadataForReport(
        report: Report?,
        context: ExecutionContext,
        workflowEngine: WorkflowEngine
    ) {
        if (report != null && report.schema.topic.lowercase() == "covid-19") {
            // next check that we're dealing with an external file
            val clientSource = report.sources.firstOrNull { it is ClientSource }
            if (clientSource != null) {
                context.logger.info("Writing deidentified report data to the DB")
                // wrap the insert into an exception handler
                try {
                    workflowEngine.db.transact { txn ->
                        // verify the file exists in report_file before continuing
                        if (workflowEngine.db.checkReportExists(report.id, txn)) {
                            val deidentifiedData = report.getDeidentifiedResultMetaData()
                            workflowEngine.db.saveTestData(deidentifiedData, txn)
                            context.logger.info("Wrote ${deidentifiedData.count()} rows to test data table")
                        } else {
                            // warn if it does not exist
                            context.logger.warning(
                                "Skipping write to metadata table because " +
                                    "reportId ${report.id} does not exist in report_file."
                            )
                        }
                    }
                } catch (pse: PSQLException) {
                    // report this but move on
                    context.logger.severe(
                        "Exception writing COVID test metadata " +
                            "for ${report.id}: ${pse.localizedMessage}.\n" +
                            pse.stackTraceToString()
                    )
                }
            }
        }
    }

    private fun validateRequest(engine: WorkflowEngine, request: HttpRequestMessage<String?>): ValidatedRequest {
        val errors = mutableListOf<ResultDetail>()
        val warnings = mutableListOf<ResultDetail>()

        val (sizeStatus, errMsg) = HttpUtilities.payloadSizeCheck(request)
        if (sizeStatus != HttpStatus.OK) {
            errors.add(ResultDetail.report(errMsg))
            // If size is too big, we ignore the option.
            return ValidatedRequest(sizeStatus, errors, warnings)
        }

        val optionsText = request.queryParameters.getOrDefault(OPTION_PARAMETER, "")
        val options = if (optionsText.isNotBlank()) {
            try {
                Options.valueOf(optionsText)
            } catch (e: IllegalArgumentException) {
                errors.add(ResultDetail.param(OPTION_PARAMETER, "'$optionsText' is not valid"))
                Options.None
            }
        } else {
            Options.None
        }

        if (options == Options.CheckConnections) {
            return ValidatedRequest(HttpStatus.OK, errors, warnings, options = options)
        }

        val receiverNamesText = request.queryParameters.getOrDefault(ROUTE_TO_PARAMETER, "")
        val routeTo = if (receiverNamesText.isNotBlank()) receiverNamesText.split(ROUTE_TO_SEPARATOR) else emptyList()
        val receiverNameErrors = routeTo
            .filter { engine.settings.findReceiver(it) == null }
            .map { ResultDetail.param(ROUTE_TO_PARAMETER, "Invalid receiver name: $it") }
        errors.addAll(receiverNameErrors)

        val clientName = request.headers[CLIENT_PARAMETER] ?: request.queryParameters.getOrDefault(CLIENT_PARAMETER, "")
        if (clientName.isBlank())
            errors.add(ResultDetail.param(CLIENT_PARAMETER, "Expected a '$CLIENT_PARAMETER' query parameter"))

        val sender = engine.settings.findSender(clientName)
        if (sender == null)
            errors.add(ResultDetail.param(CLIENT_PARAMETER, "'$CLIENT_PARAMETER:$clientName': unknown sender"))

        val schema = engine.metadata.findSchema(sender?.schemaName ?: "")
        if (sender != null && schema == null)
            errors.add(
                ResultDetail.param(
                    CLIENT_PARAMETER, "'$CLIENT_PARAMETER:$clientName': unknown schema '${sender.schemaName}'"
                )
            )

        val contentType = request.headers.getOrDefault(HttpHeaders.CONTENT_TYPE.lowercase(), "")
        if (contentType.isBlank()) {
            errors.add(ResultDetail.param(HttpHeaders.CONTENT_TYPE, "missing"))
        } else if (sender != null && sender.format.mimeType != contentType) {
            errors.add(ResultDetail.param(HttpHeaders.CONTENT_TYPE, "expecting '${sender.format.mimeType}'"))
        }

        val content = request.body ?: ""
        if (content.isEmpty()) {
            errors.add(ResultDetail.param("Content", "expecting a post message with content"))
        }

        if (sender == null || schema == null || content.isEmpty() || errors.isNotEmpty()) {
            return ValidatedRequest(HttpStatus.BAD_REQUEST, errors, warnings)
        }

        val defaultValues = if (request.queryParameters.containsKey(DEFAULT_PARAMETER)) {
            val values = request.queryParameters.getOrDefault(DEFAULT_PARAMETER, "").split(",")
            values.mapNotNull {
                val parts = it.split(DEFAULT_SEPARATOR)
                if (parts.size != 2) {
                    errors.add(ResultDetail.report("'$it' is not a valid default"))
                    return@mapNotNull null
                }
                val element = schema.findElement(parts[0])
                if (element == null) {
                    errors.add(ResultDetail.report("'${parts[0]}' is not a valid element name"))
                    return@mapNotNull null
                }
                val error = element.checkForError(parts[1])
                if (error != null) {
                    errors.add(ResultDetail.param(DEFAULT_PARAMETER, error))
                    return@mapNotNull null
                }
                Pair(parts[0], parts[1])
            }.toMap()
        } else {
            emptyMap()
        }

        if (content.isEmpty() || errors.isNotEmpty()) {
            return ValidatedRequest(HttpStatus.BAD_REQUEST, errors, warnings)
        }

        var report = createReport(engine, sender, content, defaultValues, errors, warnings)
        var status = HttpStatus.OK
        if (options != Options.SkipInvalidItems && errors.isNotEmpty()) {
            report = null
            status = HttpStatus.BAD_REQUEST
        }
        return ValidatedRequest(status, errors, warnings, options, defaultValues, routeTo, report)
    }

    private fun createReport(
        engine: WorkflowEngine,
        sender: Sender,
        content: String,
        defaults: Map<String, String>,
        errors: MutableList<ResultDetail>,
        warnings: MutableList<ResultDetail>
    ): Report? {
        return when (sender.format) {
            Sender.Format.CSV -> {
                try {
                    val readResult = engine.csvSerializer.readExternal(
                        schemaName = sender.schemaName,
                        input = ByteArrayInputStream(content.toByteArray()),
                        sources = listOf(ClientSource(organization = sender.organizationName, client = sender.name)),
                        defaultValues = defaults
                    )
                    errors += readResult.errors
                    warnings += readResult.warnings
                    readResult.report
                } catch (e: Exception) {
                    errors.add(ResultDetail.report(e.message ?: ""))
                    null
                }
            }
            Sender.Format.HL7 -> {
                try {
                    val readResult = engine.hl7Serializer.readExternal(
                        schemaName = sender.schemaName,
                        input = ByteArrayInputStream(content.toByteArray()),
                        ClientSource(organization = sender.organizationName, client = sender.name)
                    )
                    errors += readResult.errors
                    warnings += readResult.warnings
                    readResult.report
                } catch (e: Exception) {
                    errors.add(ResultDetail.report(e.message ?: ""))
                    null
                }
            }
        }
    }

    private fun routeReport(
        context: ExecutionContext,
        workflowEngine: WorkflowEngine,
        validatedRequest: ValidatedRequest,
        actionHistory: ActionHistory,
    ) {
        if (validatedRequest.options == Options.ValidatePayload ||
            validatedRequest.options == Options.CheckConnections
        ) return
        workflowEngine.db.transact { txn ->
            workflowEngine
                .translator
                .filterAndTranslateByReceiver(
                    validatedRequest.report!!,
                    validatedRequest.defaults,
                    validatedRequest.routeTo,
                    validatedRequest.warnings,
                )
                .forEach { (report, receiver) ->
                    sendToDestination(
                        report,
                        receiver,
                        context,
                        workflowEngine,
                        validatedRequest,
                        actionHistory,
                        txn
                    )
                }
        }
    }

    private fun sendToDestination(
        report: Report,
        receiver: Receiver,
        context: ExecutionContext,
        workflowEngine: WorkflowEngine,
        validatedRequest: ValidatedRequest,
        actionHistory: ActionHistory,
        txn: DataAccessTransaction
    ) {
        val loggerMsg: String
        when {
            validatedRequest.options == Options.SkipSend -> {
                // Note that SkipSend should really be called SkipBothTimingAndSend  ;)
                val event = ReportEvent(Event.EventAction.NONE, report.id)
                workflowEngine.dispatchReport(event, report, actionHistory, receiver, txn, context)
                loggerMsg = "Queue: ${event.toQueueMessage()}"
            }
            receiver.timing != null && validatedRequest.options != Options.SendImmediately -> {
                val time = receiver.timing.nextTime()
                // Always force a batched report to be saved in our INTERNAL format
                val batchReport = report.copy(bodyFormat = Report.Format.INTERNAL)
                val event = ReceiverEvent(Event.EventAction.BATCH, receiver.fullName, time)
                workflowEngine.dispatchReport(event, batchReport, actionHistory, receiver, txn, context)
                loggerMsg = "Queue: ${event.toQueueMessage()}"
            }
            receiver.format == Report.Format.HL7 -> {
                report
                    .split()
                    .forEach {
                        val event = ReportEvent(Event.EventAction.SEND, it.id)
                        workflowEngine.dispatchReport(event, it, actionHistory, receiver, txn, context)
                    }
                loggerMsg = "Queued to send immediately: HL7 split into ${report.itemCount} individual reports"
            }
            else -> {
                val event = ReportEvent(Event.EventAction.SEND, report.id)
                workflowEngine.dispatchReport(event, report, actionHistory, receiver, txn, context)
                loggerMsg = "Queued to send immediately: ${event.toQueueMessage()}"
            }
        }
        context.logger.info(loggerMsg)
    }

    // todo I think all of this info is now in ActionHistory.  Move to there.   Already did destinations.
    private fun createResponseBody(
        result: ValidatedRequest,
        actionHistory: ActionHistory? = null,
    ): String {
        val factory = JsonFactory()
        val outStream = ByteArrayOutputStream()
        factory.createGenerator(outStream).use {
            it.useDefaultPrettyPrinter()
            it.writeStartObject()
            if (result.report != null) {
                it.writeStringField("id", result.report.id.toString())
                it.writeStringField("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
                it.writeStringField("topic", result.report.schema.topic.toString())
                it.writeNumberField("reportItemCount", result.report.itemCount)
            } else
                it.writeNullField("id")

            actionHistory?.prettyPrintDestinationsJson(it, WorkflowEngine.settings, result.options)

            it.writeNumberField("warningCount", result.warnings.size)
            it.writeNumberField("errorCount", result.errors.size)

            fun writeDetailsArray(field: String, array: List<ResultDetail>) {
                it.writeArrayFieldStart(field)
                array.forEach { error ->
                    it.writeStartObject()
                    it.writeStringField("scope", error.scope.toString())
                    it.writeStringField("id", error.id)
                    it.writeStringField("details", error.details)
                    it.writeEndObject()
                }
                it.writeEndArray()
            }
            writeDetailsArray("errors", result.errors)
            writeDetailsArray("warnings", result.warnings)
            it.writeEndObject()
        }
        return outStream.toString()
    }
}