package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import com.okta.jwt.JwtVerifiers
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.secrets.SecretManagement
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.StringTemplateResolver
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.UUID

class DownloadFunction() : SecretManagement, BaseHistoryFunction() {
    val LOGIN_PAGE = "./assets/csv-download-site/login__inline.html"
    val DOWNLOAD_PAGE = "./assets/csv-download-site/index__inline.html"
    val FILENOTFOUND_PAGE = "./assets/csv-download-site/nosuchfile__inline.html"

    data class TestResult(
        val date: String?,
        val receiver: String?,
        val expires: Long?,
        val total: Any? = null,
        val positive: Any? = null,
        val file: String? = null,
        val format: String? = null,
        val fileName: String? = null,
    )

    data class AuthClaims(
        val userName: String,
        val organization: Organization
    )

    @FunctionName("download")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        var authClaims = checkAuthenticatedCookie(request, context)
        if (authClaims != null) {
            val file: String = request.queryParameters["file"] ?: ""
            if (file.isBlank())
                return responsePage(request, authClaims)
            else
                return responseFile(request, file, authClaims, context)
        } else {
            return serveAuthenticatePage(request)
        }
    }

    private fun serveAuthenticatePage(request: HttpRequestMessage<String?>): HttpResponseMessage {
        val htmlTemplate = Files.readString(Path.of(LOGIN_PAGE))

        val attr = mapOf(
            "OKTA_baseUrl" to System.getenv("OKTA_baseUrl"),
            "OKTA_clientId" to secretService.fetchSecret("OKTA_clientId"),
            "OKTA_redirect" to System.getenv("OKTA_redirect")
        )

        val html = getTemplateFromAttributes(htmlTemplate, attr)
        var response = request.createResponseBuilder(HttpStatus.OK)
            .body(html)
            .header("Content-Type", "text/html")
            .build()

        return response
    }

    private fun generateTestResults(reportFiles: List<ReportFile>, authClaims: AuthClaims): List<TestResult> {
        return reportFiles.sortedByDescending {
            it.createdAt
        }.map {
            val svc = WorkflowEngine().settings.findReceiver(it.receivingOrg + "." + it.receivingOrgSvc)
            val orgDesc = authClaims.organization.description
            val receiver = if (svc !== null && svc.description.isNotBlank()) svc.description else orgDesc
            TestResult(
                it.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                receiver,
                DAYS_TO_SHOW - it.createdAt.until(OffsetDateTime.now(), ChronoUnit.DAYS),
                it.itemCount,
                0,
                it.reportId.toString(),
                it.bodyFormat
            )
        }
    }

    private fun generateTodaysTestResults(
        reportFiles: List<ReportFile>,
        authClaims: AuthClaims
    ): List<TestResult> {
        var filtered = reportFiles.filter { filter(it) }
        return generateTestResults(filtered, authClaims)
    }

    private fun filter(reportFile: ReportFile): Boolean {
        val now = OffsetDateTime.now()
        return reportFile.createdAt.year == now.year &&
            reportFile.createdAt.monthValue == now.monthValue &&
            reportFile.createdAt.dayOfMonth == now.dayOfMonth
    }

    private fun generatePreviousTestResults(
        reportFiles: List<ReportFile>,
        authClaims: AuthClaims
    ): List<TestResult> {
        var filtered = reportFiles.filterNot { filter(it) }
        return generateTestResults(filtered, authClaims)
    }

    private fun responsePage(
        request: HttpRequestMessage<String?>,
        authClaims: AuthClaims
    ): HttpResponseMessage {
        val htmlTemplate: String = Files.readString(Path.of(DOWNLOAD_PAGE))
        val reportFiles = workflowEngine.fetchDownloadableReportFiles(
            OffsetDateTime.now().minusDays(DAYS_TO_SHOW), authClaims.organization.name
        )
        val attr = mapOf(
            "description" to (authClaims.organization.description),
            "user" to authClaims.userName,
            "today" to Calendar.getInstance(),
            "todays" to generateTodaysTestResults(reportFiles, authClaims),
            "previous" to generatePreviousTestResults(reportFiles, authClaims),
            "days_to_show" to DAYS_TO_SHOW,
            "OKTA_redirect" to System.getenv("OKTA_redirect"),
            "showTables" to true
        )

        val html = getTemplateFromAttributes(htmlTemplate, attr)

        var response = request.createResponseBuilder(HttpStatus.OK)
            .body(html)
            .header("Content-Type", "text/html")
            .build()

        return response
    }

    private fun responseFile(
        request: HttpRequestMessage<String?>,
        requestedFile: String,
        authClaims: AuthClaims,
        context: ExecutionContext
    ): HttpResponseMessage {
        var response: HttpResponseMessage
        try {
            val reportId = ReportId.fromString(requestedFile)
            val header = workflowEngine.fetchHeader(reportId, authClaims.organization)
            if (header.content == null || header.content.isEmpty())
                response = responsePage(request, authClaims)
            else {
                val filename = Report.formExternalFilename(header)
                val mimeType = Report.Format.safeValueOf(header.reportFile.bodyFormat).mimeType
                response = request
                    .createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", mimeType)
                    .header("Content-Disposition", "attachment; filename=$filename")
                    .body(header.content)
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
                    authClaims.userName,
                )
                actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, externalReportId))
                WorkflowEngine().recordAction(actionHistory)
                return response
            }
        } catch (ex: Exception) {
            context.logger.warning("Exception during download of $requestedFile")
            response = request.createResponseBuilder(HttpStatus.NOT_FOUND)
                .body("File $requestedFile not found")
                .header("Content-Type", "text/html")
                .build()
        }
        return response
    }

    private fun getTemplateEngine(): TemplateEngine {
        val templateEngine = TemplateEngine()
        val stringTemplateResolver = StringTemplateResolver()
        templateEngine.setTemplateResolver(stringTemplateResolver)
        return templateEngine
    }

    private fun getTemplateFromAttributes(htmlContent: String, attr: Map<String, Any?>): String {
        val templateEngine = getTemplateEngine()
        val context = Context()
        if (!attr.isEmpty())
            attr.forEach { (k, v) -> context.setVariable(k, v) }
        return templateEngine.process(htmlContent, context)
    }

    /**
     * returns null if not authorized, otherwise returns a set of claims.
     */
    fun checkAuthenticatedCookie(request: HttpRequestMessage<String?>, context: ExecutionContext): AuthClaims? {
        var userName = ""
        var orgName = ""
        val cookies = request.headers["cookie"] ?: ""
        var jwtString: String
        cookies.replace(" ", "").split(";").forEach {
            val cookie = it.split("=")
            jwtString = if (cookie[0] == "jwt") cookie[1] else ""
            if (jwtString.isNotBlank()) {
                try {
                    val jwtVerifier = JwtVerifiers.accessTokenVerifierBuilder()
                        .setIssuer("https://${System.getenv("OKTA_baseUrl")}/oauth2/default")
                        .build()
                    val jwt = jwtVerifier.decode(jwtString)
                    userName = jwt.getClaims().get("sub").toString()
                    val orgs = jwt.getClaims().get("organization")
                    var org = if (orgs !== null) (orgs as List<*>)[0] else ""
                    orgName = when (org) {
                        is String -> if (org.length > 3) org.substring(2) else ""
                        else -> ""
                    }
                    // orgName = if (org.length > 3) org.substring(2) else ""
                } catch (ex: Throwable) {
                    println(ex)
                }
            }
        }
        if (userName.isNotBlank() && orgName.isNotBlank()) {
            val organization = WorkflowEngine().settings.findOrganization(orgName.replace('_', '-'))
            if (organization != null) {
                return AuthClaims(userName, organization)
            } else {
                context.logger.info("User $userName failed auth: Organization $orgName is unknown to the system.")
            }
        }
        return null
    }
}