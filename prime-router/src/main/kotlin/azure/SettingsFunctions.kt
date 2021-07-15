package gov.cdc.prime.router.azure

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.prime.router.tokens.OktaAuthentication
import org.apache.logging.log4j.kotlin.Logging

/*
 * Organizations API
 */

class GetOrganizations(settingsFacade: SettingsFacade = SettingsFacade.common,
                       oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN)) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("getOrganizations")
    fun run(
        @HttpTrigger(
            name = "getOrganizations",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations"
        ) request: HttpRequestMessage<String?>,
    ): HttpResponseMessage {
        return getList(
            request,
            OrganizationAPI::class.java
        )
    }
}

class GetOneOrganization(settingsFacade: SettingsFacade = SettingsFacade.common,
                         oktaAuthentication: OktaAuthentication = OktaAuthentication()) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("getOneOrganization")
    fun run(
        @HttpTrigger(
            name = "getOneOrganization",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
    ): HttpResponseMessage {
        return getOne(request, organizationName, OrganizationAPI::class.java)
    }
}

class UpdateOrganization(settingsFacade: SettingsFacade = SettingsFacade.common,
                         oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.SYSTEM_ADMIN)) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("updateOneOrganization")
    fun run(
        @HttpTrigger(
            name = "updateOneOrganization",
            methods = [HttpMethod.DELETE, HttpMethod.PUT],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
    ): HttpResponseMessage {
        return updateOne(
            request,
            organizationName,
            OrganizationAPI::class.java
        )
    }
}

/**
 * Sender APIs
 */
class GetSenders(settingsFacade: SettingsFacade = SettingsFacade.common,
                 oktaAuthentication: OktaAuthentication = OktaAuthentication()) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("getSenders")
    fun run(
        @HttpTrigger(
            name = "getSenders",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/senders"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
    ): HttpResponseMessage {
        return getList(request, organizationName, SenderAPI::class.java)
    }
}

class GetOneSender(settingsFacade: SettingsFacade = SettingsFacade.common,
                   oktaAuthentication: OktaAuthentication = OktaAuthentication()) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("getOneSender")
    fun run(
        @HttpTrigger(
            name = "getOneSender",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/senders/{senderName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("senderName") senderName: String,
    ): HttpResponseMessage {
        return getOne(request, senderName, SenderAPI::class.java, organizationName)
    }
}

class UpdateSender(settingsFacade: SettingsFacade = SettingsFacade.common,
                   oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.ORGANIZATION_ADMIN)) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("updateOneSender")
    fun run(
        @HttpTrigger(
            name = "updateOneSender",
            methods = [HttpMethod.DELETE, HttpMethod.PUT],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/senders/{senderName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("senderName") senderName: String,
    ): HttpResponseMessage {
        return updateOne(
            request,
            senderName,
            SenderAPI::class.java,
            organizationName
        )
    }
}

/**
 * Receiver APIS
 */

class GetReceiver(settingsFacade: SettingsFacade = SettingsFacade.common,
                  oktaAuthentication: OktaAuthentication = OktaAuthentication()) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("getReceivers")
    fun run(
        @HttpTrigger(
            name = "getReceivers",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/receivers"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
    ): HttpResponseMessage {
        return getList(request, organizationName, ReceiverAPI::class.java)
    }
}

class GetOneReceiver(settingsFacade: SettingsFacade = SettingsFacade.common,
                     oktaAuthentication: OktaAuthentication = OktaAuthentication()) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("getOneReceiver")
    fun run(
        @HttpTrigger(
            name = "getOneReceiver",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/receivers/{receiverName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("receiverName") receiverName: String,
    ): HttpResponseMessage {
        return getOne(request, receiverName, ReceiverAPI::class.java, organizationName)
    }
}

class UpdateReceiver(settingsFacade: SettingsFacade = SettingsFacade.common,
                     oktaAuthentication: OktaAuthentication = OktaAuthentication(PrincipalLevel.ORGANIZATION_ADMIN)) :
    BaseFunction(settingsFacade, oktaAuthentication) {
    @FunctionName("updateOneReceiver")
    fun run(
        @HttpTrigger(
            name = "updateOneReceiver",
            methods = [HttpMethod.DELETE, HttpMethod.PUT],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "settings/organizations/{organizationName}/receivers/{receiverName}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("organizationName") organizationName: String,
        @BindingName("receiverName") receiverName: String,
    ): HttpResponseMessage {
        return updateOne(
            request,
            receiverName,
            ReceiverAPI::class.java,
            organizationName
        )
    }
}

/**
 * Common Settings API
 */

open class BaseFunction(
    private val facade: SettingsFacade,
    private val oktaAuthentication: OktaAuthentication
) : Logging {
    private val missingAuthorizationHeader = HttpUtilities.errorJson("Missing Authorization Header")
    private val invalidClaim = HttpUtilities.errorJson("Invalid Authorization Header")

    fun <T : SettingAPI> getList(
        request: HttpRequestMessage<String?>,
        clazz: Class<T>
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, "") {
            val settings = facade.findSettingsAsJson(clazz)
            HttpUtilities.okResponse(request, settings)
        }
    }

    fun <T : SettingAPI> getList(
        request: HttpRequestMessage<String?>,
        organizationName: String,
        clazz: Class<T>
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, "") {
            val (result, outputBody) = facade.findSettingsAsJson(organizationName, clazz)
            facadeResultToResponse(request, result, outputBody)
        }
    }

    fun <T : SettingAPI> getOne(
        request: HttpRequestMessage<String?>,
        settingName: String,
        clazz: Class<T>,
        organizationName: String? = null
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, organizationName ?: settingName) {
            val setting = facade.findSettingAsJson(settingName, clazz, organizationName)
                ?: return@checkAccess HttpUtilities.notFoundResponse(request)
            HttpUtilities.okResponse(request, setting)
        }
    }

    fun <T : SettingAPI> updateOne(
        request: HttpRequestMessage<String?>,
        settingName: String,
        clazz: Class<T>,
        organizationName: String? = null
    ): HttpResponseMessage {
        return oktaAuthentication.checkAccess(request, organizationName ?: settingName) { claims ->
            val (result, outputBody) = when (request.httpMethod) {
                HttpMethod.PUT -> {
                    if (request.headers[HttpHeaders.CONTENT_TYPE.lowercase()] != HttpUtilities.jsonMediaType)
                        return@checkAccess HttpUtilities.badRequestResponse(request, errorJson("invalid media type"))
                    val body = request.body
                        ?: return@checkAccess HttpUtilities.badRequestResponse(request, errorJson("missing payload"))
                    facade.putSetting(settingName, body, claims, clazz, organizationName)
                }
                HttpMethod.DELETE ->
                    facade.deleteSetting(settingName, claims, clazz, organizationName)
                else ->
                    return@checkAccess HttpUtilities.badRequestResponse(request, errorJson("unsupported method"))
            }
            facadeResultToResponse(request, result, outputBody)
        }
    }

    private fun facadeResultToResponse(
        request: HttpRequestMessage<String?>,
        result: SettingsFacade.AccessResult,
        outputBody: String
    ): HttpResponseMessage {
        return when (result) {
            SettingsFacade.AccessResult.SUCCESS -> HttpUtilities.okResponse(request, outputBody)
            SettingsFacade.AccessResult.CREATED -> HttpUtilities.createdResponse(request, outputBody)
            SettingsFacade.AccessResult.NOT_FOUND -> HttpUtilities.notFoundResponse(request)
            SettingsFacade.AccessResult.BAD_REQUEST -> HttpUtilities.badRequestResponse(request, outputBody)
        }
    }

    private fun errorJson(message: String): String = HttpUtilities.errorJson(message)

}