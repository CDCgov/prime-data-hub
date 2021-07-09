package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.azure.db.enums.TaskAction
import org.apache.logging.log4j.kotlin.Logging
import java.util.UUID

/*
 * Requeue API
 */

class RequeueFunction : Logging {
    @FunctionName("requeue") // devnote:  putting slashes in this (/) breaks it.
    fun run(
        @HttpTrigger(
            name = "requeue",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.FUNCTION,
            route = "requeue/send"
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        logger.info("Entering requeue/send api")
        val workflowEngine = WorkflowEngine()
        val actionHistory = ActionHistory(TaskAction.resend, context)
        actionHistory.trackActionParams(request)
        var msgs = mutableListOf<String>()
        val response = try {
            doResend(request, workflowEngine, msgs)
        } catch (t: Throwable) {
            msgs.add(t.cause?.let { "${t.cause!!.localizedMessage}\n" } ?: "" + t.localizedMessage)
            HttpUtilities.bad(request, msgs.joinToString("\n") + "\n")
        }
        actionHistory.trackActionResult(response)
        workflowEngine.recordAction(actionHistory)
        return response
    }

    fun doResend(
        request: HttpRequestMessage<String?>,
        workflowEngine: WorkflowEngine,
        msgs: MutableList<String>,
    ): HttpResponseMessage {
        val isTest = ! request.queryParameters["test"].isNullOrEmpty()
        if (isTest) msgs.add("Here is what would happen if this were NOT a test:")
        if (request.queryParameters.size < 2 || request.queryParameters.size > 4)
            return HttpUtilities.bad(request, "Expecting 2 to 4 parameters\n")
        val reportIdStr = request.queryParameters["reportId"]
            ?: return HttpUtilities.bad(request, "Missing option reportId\n")
        val reportId = UUID.fromString(reportIdStr)
        val fullName = request.queryParameters["receiver"]
            ?: return HttpUtilities.bad(request, "Missing option receiver\n")
        val isFailedOnly = ! request.queryParameters["failedOnly"].isNullOrEmpty()
        val receiver = workflowEngine.settings.findReceiver(fullName)
            ?: return HttpUtilities.bad(request, "No such receiver fullname $fullName\n")
        // sanity checks throw exceptions inside here:
        workflowEngine.resendEvent(reportId, receiver, isFailedOnly, isTest, msgs)
        return HttpUtilities.httpResponse(request, msgs.joinToString("\n") + "\n", HttpStatus.OK)
    }
}