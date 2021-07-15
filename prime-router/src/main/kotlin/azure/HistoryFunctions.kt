package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import fuzzycsv.FuzzyCSVTable
import fuzzycsv.FuzzyStaticApi.count
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.WorkflowEngine.Header
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.tokens.OktaAuthentication
import java.io.StringReader
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.collections.ArrayList

class Facility private constructor(
    val organization: String?,
    val facility: String?,
    val CLIA: String?,
    val positive: Long?,
    val total: Long?
) {

    data class Builder(
        var organization: String? = null,
        var facility: String? = null,
        var CLIA: String? = null,
        var positive: Long? = null,
        var total: Long? = null
    ) {

        fun organization(organization: String) = apply { this.organization = organization }
        fun facility(facility: String) = apply { this.facility = facility }
        fun CLIA(CLIA: String) = apply { this.CLIA = CLIA }
        fun positive(positive: Long) = apply { this.positive = positive }
        fun total(total: Long) = apply { this.total = total }
        fun build() = Facility(organization, facility, CLIA, positive, total)
    }
}

class Action private constructor(
    val date: String?,
    val user: String?,
    val action: String?
) {

    data class Builder(
        var date: String? = null,
        var user: String? = null,
        var action: String? = null
    ) {

        fun date(date: String) = apply { this.date = date }
        fun user(user: String) = apply { this.user = user }
        fun action(action: String) = apply { this.action = action }
        fun build() = Action(date, user, action)
    }
}

class ReportView private constructor(
    val sent: Long?,
    val via: String?,
    val positive: Long?,
    val total: Long?,
    val fileType: String?,
    val type: String?,
    val reportId: String?,
    val expires: Long?,
    val sendingOrg: String?,
    val receivingOrg: String?,
    val facilities: ArrayList<Facility>?,
    val actions: ArrayList<Action>?
) {

    data class Builder(
        var sent: Long? = null,
        var via: String? = null,
        var positive: Long? = null,
        var total: Long? = null,
        var fileType: String? = null,
        var type: String? = null,
        var reportId: String? = null,
        var expires: Long? = null,
        var sendingOrg: String? = null,
        var receivingOrg: String? = null,
        var facilities: ArrayList<Facility>? = ArrayList<Facility>(),
        var actions: ArrayList<Action>? = ArrayList<Action>()
    ) {

        fun sent(sent: Long) = apply { this.sent = sent }
        fun via(via: String) = apply { this.via = via }
        fun positive(positive: Long) = apply { this.positive = positive }
        fun total(total: Long) = apply { this.total = total }
        fun fileType(fileType: String) = apply { this.fileType = fileType }
        fun type(type: String) = apply { this.type = type }
        fun reportId(reportId: String) = apply { this.reportId = reportId }
        fun expires(expires: Long) = apply { this.expires = expires }
        fun sendingOrg(sendingOrg: String) = apply { this.sendingOrg = sendingOrg }
        fun receivingOrg(receivingOrg: String) = apply { this.receivingOrg = receivingOrg }
        fun facilities(facilities: ArrayList<Facility>) = apply { this.facilities = facilities }
        fun actions(actions: ArrayList<Action>) = apply { this.actions = actions }
        fun build() = ReportView(
            sent,
            via,
            positive,
            total,
            fileType,
            type,
            reportId,
            expires,
            sendingOrg,
            receivingOrg,
            facilities,
            actions
        )
    }
}

class CardView private constructor(
    val id: String?,
    val title: String?,
    val subtitle: String?,
    val daily: Long?,
    val last: Double?,
    val positive: Boolean?,
    val change: Double?,
    val pct_change: Double?,
    val data: Array<Long>?
) {

    data class Builder(
        var id: String? = null,
        var title: String? = null,
        var subtitle: String? = null,
        var daily: Long? = null,
        var last: Double? = null,
        var positive: Boolean? = null,
        var change: Double? = null,
        var pct_change: Double? = null,
        var data: Array<Long>? = emptyArray<Long>()
    ) {

        fun id(id: String) = apply { this.id = id }
        fun title(title: String) = apply { this.title = title }
        fun subtitle(subtitle: String) = apply { this.subtitle = subtitle }
        fun daily(daily: Long) = apply { this.daily = daily }
        fun last(last: Double) = apply { this.last = last }
        fun positive(positive: Boolean) = apply { this.positive = positive }
        fun change(change: Double) = apply { this.change = change }
        fun pct_change(pct_change: Double) = apply { this.pct_change = pct_change }
        fun data(data: Array<Long>) = apply { this.data = data }
        fun build() = CardView(id, title, subtitle, daily, last, positive, change, pct_change, data)
    }
}

data class FileReturn(val content: String, val filename: String, val mimetype: String)

class GetReports(oktaAuthentication: OktaAuthentication = OktaAuthentication()) :
    BaseHistoryFunction(oktaAuthentication) {

    @FunctionName("getReports")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "getReports",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/report"
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        return GetReports(request, context)
    }
}

class GetReportById(oktaAuthentication: OktaAuthentication = OktaAuthentication()) :
    BaseHistoryFunction(oktaAuthentication) {
    @FunctionName("getReportById")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "getReportById",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/report/{reportId}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("reportId") reportId: String,
        context: ExecutionContext,
    ): HttpResponseMessage {
        return GetReportById(request, reportId, context)
    }
}

class GetSummaryTests(oktaAuthentication: OktaAuthentication = OktaAuthentication()) :
    BaseHistoryFunction(oktaAuthentication) {

    @FunctionName("getSummaryTests")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "getSummaryTest",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/summary/tests"
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext
    ): HttpResponseMessage {
        return GetSummaryTests(request, context)
    }
}

class GetSummary(oktaAuthentication: OktaAuthentication = OktaAuthentication())
                 : BaseHistoryFunction(oktaAuthentication) {
    @FunctionName("getSummary")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "getSummary",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "history/summary/field/{field}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("field") field: String,
        context: ExecutionContext
    ): HttpResponseMessage {
        return GetSummary(request, field, context)
    }
}

open class BaseHistoryFunction(private val oktaAuthentication: OktaAuthentication) {

    val DAYS_TO_SHOW = 30L
    val workflowEngine = WorkflowEngine()

    fun GetReports(request: HttpRequestMessage<String?>, context: ExecutionContext): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, request.headers["organization"] ?: "") { claims ->
            val authOrganization = oktaAuthentication.checkOrganizationExists(context, claims.userName, claims.organizationName)
                ?: return@checkAccess request.createResponseBuilder(HttpStatus.UNAUTHORIZED).build()

            var response: HttpResponseMessage
            try {
                val headers = workflowEngine.db.fetchDownloadableReportFiles(
                    OffsetDateTime.now().minusDays(DAYS_TO_SHOW), claims.organizationName ?: ""
                )

                @Suppress("NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER")
                var reports = headers.sortedByDescending { it.createdAt }.map {

                    var facilities = arrayListOf<Facility>()
                    if (it.bodyFormat == "CSV")
                        try {
                            facilities = getFieldSummaryForReportId(
                                arrayOf("Testing_lab_name", "Testing_lab_CLIA"), it.reportId.toString(), authOrganization
                            )
                        } catch (ex: Exception) {
                            // context.logger.info( "Exception during getFieldSummaryForReportId - TestingLabName was not found - no facilities data will be published" );
                        }

                    var actions = getActionsForReportId(it.reportId.toString(), authOrganization)

                    ReportView.Builder()
                        .reportId(it.reportId.toString())
                        .sent(it.createdAt.toEpochSecond() * 1000)
                        .via(it.bodyFormat)
                        .total(it.itemCount.toLong())
                        .fileType(it.bodyFormat)
                        .type("ELR")
                        .expires(it.createdAt.plusDays(DAYS_TO_SHOW).toEpochSecond() * 1000)
                        .facilities(facilities)
                        .actions(actions)
                        .build()
                }

                response = request.createResponseBuilder(HttpStatus.OK)
                    .body(reports)
                    .header("Content-Type", "application/json")
                    .build()
            } catch (ex: Exception) {
                context.logger.info("Exception during creating of reports list - file not found")
                response = request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body("File not found")
                    .header("Content-Type", "text/html")
                    .build()
            }
            return@checkAccess response
        }

    }

    fun GetReportById(
        request: HttpRequestMessage<String?>,
        reportIdIn: String,
        context: ExecutionContext
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, request.headers["organization"] ?: "") { claims ->
            val authOrganization = oktaAuthentication.checkOrganizationExists(context, claims.userName, claims.organizationName)
                ?: return@checkAccess request.createResponseBuilder(HttpStatus.UNAUTHORIZED).build()

            var response: HttpResponseMessage
            try {
                val reportId = ReportId.fromString(reportIdIn)
                val header = workflowEngine.fetchHeader(reportId, authOrganization)
                if (header.content == null || header.content.isEmpty())
                    response = request.createResponseBuilder(HttpStatus.NOT_FOUND).build()
                else {
                    val filename = Report.formExternalFilename(header)
                    val mimeType = Report.Format.safeValueOf(header.reportFile.bodyFormat).mimeType

                    val fileReturn = FileReturn(String(header.content), filename, mimeType)
                    response = request
                        .createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(fileReturn)
                        .build()

                    val actionHistory = ActionHistory(TaskAction.download, context)
                    actionHistory.trackActionRequestResponse(request, response)
                    // Give the external report_file a new UUID, so we can track its history distinct from the
                    // internal blob.   This is going to be very confusing.
                    val externalReportId = UUID.randomUUID()
                    actionHistory.trackDownloadedReport(
                        header,
                        filename,
                        externalReportId,
                        claims.userName,
                    )
                    actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, externalReportId))
                    WorkflowEngine().recordAction(actionHistory)

                    return@checkAccess response
                }
            } catch (ex: Exception) {
                context.logger.warning("Exception during download of $reportIdIn - file not found")
                response = request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body("File $reportIdIn not found")
                    .header("Content-Type", "text/html")
                    .build()
            }
            return@checkAccess response
        }

    }

    fun isToday(date: OffsetDateTime): Boolean {
        return date.monthValue == OffsetDateTime.now().monthValue &&
            date.dayOfMonth == OffsetDateTime.now().dayOfMonth &&
            date.year == OffsetDateTime.now().year
    }

    fun isYesterday(date: OffsetDateTime): Boolean {
        var yesterday = OffsetDateTime.now().minusDays(1L)
        return date.monthValue == yesterday.monthValue &&
            date.dayOfMonth == yesterday.dayOfMonth &&
            date.year == yesterday.year
    }

    fun GetSummaryTests(
        request: HttpRequestMessage<String?>,
        context: ExecutionContext
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, request.headers["organization"] ?: "") { claims ->
            val authOrganization =
                oktaAuthentication.checkOrganizationExists(context, claims.userName, claims.organizationName)
                    ?: return@checkAccess request.createResponseBuilder(HttpStatus.UNAUTHORIZED).build()
            var response: HttpResponseMessage

            try {
                val headers = workflowEngine.db.fetchDownloadableReportFiles(
                    OffsetDateTime.now().minusDays(DAYS_TO_SHOW),
                    authOrganization.name
                )
                var daily: Long = 0L
                var sum: Long = 0L
                var data: Array<Long> = arrayOf(0, 0, 0, 0, 0, 0, 0, 0)

                @Suppress("NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER")
                headers.sortedByDescending { it.createdAt }.forEach {
                    if (isToday(it.createdAt)) daily += it.itemCount.toLong()
                    sum += it.itemCount.toLong()
                    val expires: Int =
                        (DAYS_TO_SHOW - it.createdAt.until(OffsetDateTime.now(), ChronoUnit.DAYS)).toInt()
                    data.set(expires, data.get(expires) + it.itemCount.toLong())
                }

                var avg: Double = 0.0
                data.forEach { avg += it }
                avg = avg / data.size

                var card = CardView.Builder()
                    .id("summary-tests")
                    .title("Tests")
                    .subtitle("Tests reported")
                    .daily(daily)
                    .last(avg)
                    .positive(true)
                    .change(daily - avg)
                    .data(data)
                    .build()
                response = request.createResponseBuilder(HttpStatus.OK)
                    .body(card)
                    .build()
            } catch (ex: Exception) {
                context.logger.info("Exception during download of summary/tests")
                response = request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body("File not found")
                    .header("Content-Type", "text/html")
                    .build()
            }
            return@checkAccess response
        }
    }

    fun GetSummary(
        request: HttpRequestMessage<String?>,
        field: String,
        context: ExecutionContext
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, request.headers["organization"] ?: "") { claims ->
            val authOrganization =
                oktaAuthentication.checkOrganizationExists(context, claims.userName, claims.organizationName)
                    ?: return@checkAccess request.createResponseBuilder(HttpStatus.UNAUTHORIZED).build()
            var response: HttpResponseMessage
            try {
                val headers = workflowEngine.db.fetchDownloadableReportFiles(
                    OffsetDateTime.now().minusDays(DAYS_TO_SHOW), authOrganization.name
                )

                @Suppress("NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER")
                val reports = headers.sortedByDescending { it.createdAt }.map {
                    if (it.bodyFormat == "CSV") {
                        getFieldSummaryForReportId(arrayOf(field), it.reportId.toString(), authOrganization)
                    } else {
                        arrayListOf()
                    }
                }

                response = request.createResponseBuilder(HttpStatus.OK)
                    .body(reports)
                    .header("Content-Type", "application/json")
                    .build()
            } catch (ex: Exception) {
                context.logger.info("Exception during download of summary")
                response = request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Exception during GetSummary()")
                    .header("Content-Type", "text/html")
                    .build()
            }
            return@checkAccess response
        }
    }

    fun getFieldSummaryForReportId(
        fieldName: Array<String>,
        reportId: String,
        organization: Organization
    ): ArrayList<Facility> {
        var header: Header?
        var csv: FuzzyCSVTable? = null
        var facilties: ArrayList<Facility> = ArrayList<Facility>()

        try {
            header = workflowEngine.fetchHeader(ReportId.fromString(reportId), organization)
        } catch (ex: Exception) { header = null }
        if (header !== null)
            csv = FuzzyCSVTable.parseCsv(StringReader(String(header.content!!)))
        if (csv !== null) {
            csv = csv.summarize(*fieldName, count(fieldName[0]).az("Count"))
            csv.forEach {
                facilties.add(
                    Facility.Builder()
                        .facility(it.getAt(0).toString())
                        .CLIA(it.getAt(1).toString())
                        .total(it.getAt(2).toString().toLong())
                        .build()
                )
            }
        }
        return facilties
    }

    fun getActionsForReportId(reportId: String, organization: Organization): ArrayList<Action> {
        var header: Header?
        var actions: ArrayList<Action> = ArrayList<Action>()

        try {
            header = workflowEngine.fetchHeader(ReportId.fromString(reportId), organization)
        } catch (ex: Exception) { header = null }

        /* 
        if( header !== null && header.itemLineages !== null ){
            header.itemLineages
                actions.add( Action.Builder()
                                .date( it.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) )
                                .user( "USER" )
                                .action( it.transportResult )
                                .build() )                                   
            }
        }
        */
        return actions
    }
}
