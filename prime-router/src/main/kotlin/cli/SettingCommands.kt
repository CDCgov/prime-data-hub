package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.outputStream
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers.Companion.CONTENT_TYPE
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.OrganizationAPI
import gov.cdc.prime.router.azure.ReceiverAPI
import gov.cdc.prime.router.azure.SenderAPI
import org.apache.http.HttpStatus
import java.io.InputStream

private const val apiPath = "/api/settings"
private const val dummyAccessToken = "dummy"
private const val jsonMimeType = "application/json"

/**
 * Base class to handle common stuff: authentication, calling, inputs and outputs
 */
abstract class SettingCommand(
    name: String,
    help: String,
) : CliktCommand(name = name, help = help) {
    private val env by option(
        "-e", "--env",
        metavar = "<name>",
        envvar = "PRIME_ENVIRONMENT",
        help = "Connect to <name> environment.\nChoose between [local|test|staging|prod]"
    )
        .choice("local", "test", "staging", "prod")
        .default("local", "local environment")

    private val outStream by option("-o", "--output", help = "Output to file", metavar = "<file>")
        .outputStream(createIfNotExist = true, truncateExisting = true)
        .default(System.out)

    open val inStream: InputStream? = null

    data class Environment(
        val name: String,
        val baseUrl: String,
        val useHttp: Boolean = false,
        val oktaApp: OktaCommand.OktaApp? = null
    )

    enum class Operation { LIST, GET, PUT, DELETE }
    enum class SettingType { ORG, SENDER, RECEIVER }

    val jsonMapper = jacksonObjectMapper()
    val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

    init {
        // Format OffsetDateTime as an ISO string
        jsonMapper.registerModule(JavaTimeModule())
        jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        yamlMapper.registerModule(JavaTimeModule())
        yamlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun getEnvironment(): Environment {
        return environments.find { it.name == env } ?: abort("bad environment")
    }

    fun getAccessToken(environment: Environment): String {
        if (environment.oktaApp == null) return dummyAccessToken
        return OktaCommand.fetchAccessToken(environment.oktaApp)
            ?: abort("Invalid access token. Run ./prime login to fetch/refresh your access token.")
    }

    fun put(
        environment: Environment,
        accessToken: String,
        settingType: SettingType,
        settingName: String,
        payload: String
    ): String {
        val path = formPath(environment, Operation.PUT, settingType, settingName)
        val (_, response, result) = Fuel
            .put(path)
            .authentication()
            .bearer(accessToken)
            .header(CONTENT_TYPE to jsonMimeType)
            .jsonBody(payload)
            .responseJson()
        return when (result) {
            is Result.Failure -> {
                abort("Error on put of $settingName: ${response.responseMessage} ${String(response.data)}")
            }
            is Result.Success ->
                when (response.statusCode) {
                    HttpStatus.SC_OK -> {
                        val version = result.value.obj().getInt("version")
                        "Success. Setting $settingName at version $version"
                    }
                    HttpStatus.SC_CREATED -> "Success. Created $settingName"
                    else -> error("Unexpected successful status code")
                }
        }
    }

    fun delete(environment: Environment, accessToken: String, settingType: SettingType, settingName: String) {
        val path = formPath(environment, Operation.DELETE, settingType, settingName)
        val (_, response, result) = Fuel
            .delete(path)
            .authentication()
            .bearer(accessToken)
            .header(CONTENT_TYPE to jsonMimeType)
            .responseString()
        return when (result) {
            is Result.Failure ->
                abort("Error on delete of $settingName: ${response.responseMessage} ${String(response.data)}")
            is Result.Success -> Unit
        }
    }

    fun get(environment: Environment, accessToken: String, settingType: SettingType, settingName: String): String {
        val path = formPath(environment, Operation.GET, settingType, settingName)
        val (_, response, result) = Fuel
            .get(path)
            .authentication()
            .bearer(accessToken)
            .header(CONTENT_TYPE to jsonMimeType)
            .responseString()
        return when (result) {
            is Result.Failure ->
                abort("Error getting $settingName: ${response.responseMessage} ${String(response.data)}")
            is Result.Success -> result.value
        }
    }

    fun getMany(environment: Environment, accessToken: String, settingType: SettingType, settingName: String): String {
        val path = formPath(environment, Operation.LIST, settingType, settingName)
        val (_, response, result) = Fuel
            .get(path)
            .authentication()
            .bearer(accessToken)
            .header(CONTENT_TYPE to jsonMimeType)
            .responseJson()
        return when (result) {
            is Result.Failure ->
                abort("Error listing $settingName: ${response.responseMessage} ${String(response.data)}")
            is Result.Success -> "[${result.value.array().join(",\n")}]"
        }
    }

    fun listNames(
        environment: Environment,
        accessToken: String,
        settingType: SettingType,
        settingName: String
    ): List<String> {
        val path = formPath(environment, Operation.LIST, settingType, settingName)
        val (_, response, result) = Fuel
            .get(path)
            .authentication()
            .bearer(accessToken)
            .header(CONTENT_TYPE to jsonMimeType)
            .responseJson()
        return when (result) {
            is Result.Failure ->
                abort("Error listing $settingName: ${response.responseMessage} ${String(response.data)}")
            is Result.Success -> {
                val resultObjs = result.value.array()
                val names = if (settingType == SettingType.ORG) {
                    (0 until resultObjs.length())
                        .map { resultObjs.getJSONObject(it) }
                        .map { it.getString("name") }
                } else {
                    (0 until resultObjs.length())
                        .map { resultObjs.getJSONObject(it) }
                        .map { "${it.getString("organizationName")}.${it.getString("name")}" }
                }
                names.sorted()
            }
        }
    }


    fun readInput(): String {
        if (inStream == null) abort("Missing input file")
        val input = String(inStream!!.readAllBytes())
        if (input.isBlank()) abort("Blank input")
        return input
    }

    fun writeOutput(output: String) {
        outStream.write(output.toByteArray())
    }

    fun abort(message: String): Nothing {
        throw PrintMessage(message, error = true)
    }

    fun fromJson(input: String, settingType: SettingType): Pair<String, String> {
        return readStructure(input, settingType, jsonMapper)
    }

    fun fromYaml(input: String, settingType: SettingType): Pair<String, String> {
        return readStructure(input, settingType, yamlMapper)
    }

    private fun readStructure(input: String, settingType: SettingType, mapper: ObjectMapper): Pair<String, String> {
        return when (settingType) {
            SettingType.ORG -> {
                val organization = mapper.readValue(input, OrganizationAPI::class.java)
                Pair(organization.name, jsonMapper.writeValueAsString(organization))
            }
            SettingType.SENDER -> {
                val sender = mapper.readValue(input, SenderAPI::class.java)
                Pair(sender.fullName, jsonMapper.writeValueAsString(sender))
            }
            SettingType.RECEIVER -> {
                val receiver = mapper.readValue(input, ReceiverAPI::class.java)
                Pair(receiver.fullName, jsonMapper.writeValueAsString(receiver))
            }
        }
    }

    fun toYaml(output: String, settingType: SettingType): String {
        // DevNote: could be handled by inherited methods, but decided that keeping all these together was maintainable
        return when (settingType) {
            SettingType.ORG -> {
                val organization = jsonMapper.readValue(output, OrganizationAPI::class.java)
                yamlMapper.writeValueAsString(organization)
            }
            SettingType.SENDER -> {
                val sender = jsonMapper.readValue(output, SenderAPI::class.java)
                return yamlMapper.writeValueAsString(sender)
            }
            SettingType.RECEIVER -> {
                val receiver = jsonMapper.readValue(output, ReceiverAPI::class.java)
                return yamlMapper.writeValueAsString(receiver)
            }
        }
    }

    companion object {
        val environments = listOf(
            Environment("local", "localhost:7071", useHttp = true),
            Environment("test", "test.prime.cdc.gov", oktaApp = OktaCommand.OktaApp.DH_TEST),
            Environment("staging", "staging.prime.cdc.gov", oktaApp = OktaCommand.OktaApp.DH_TEST),
            Environment("prod", "prime.cdc.gov", oktaApp = OktaCommand.OktaApp.DH_PROD),
        )

        fun formPath(
            environment: Environment,
            operation: Operation,
            settingType: SettingType,
            settingName: String
        ): String {
            val protocol = if (environment.useHttp) "http" else "https"
            return "$protocol://${environment.baseUrl}$apiPath${settingPath(operation, settingType, settingName)}"
        }

        fun formPath(
            environment: Environment,
            endPoint: String,
        ): String {
            val protocol = if (environment.useHttp) "http" else "https"
            return "$protocol://${environment.baseUrl}/api/$endPoint"
        }

        fun settingPath(operation: Operation, settingType: SettingType, settingName: String): String {
            return if (operation == Operation.LIST) {
                when (settingType) {
                    SettingType.ORG -> "/organizations"
                    SettingType.SENDER -> "/organizations/$settingName/senders"
                    SettingType.RECEIVER -> "/organizations/$settingName/receivers"
                }
            } else {
                when (settingType) {
                    SettingType.ORG -> "/organizations/$settingName"
                    SettingType.SENDER -> {
                        val (orgName, senderName) = Sender.parseFullName(settingName)
                        "/organizations/$orgName/senders/$senderName"
                    }
                    SettingType.RECEIVER -> {
                        val (orgName, receiverName) = Receiver.parseFullName(settingName)
                        "/organizations/$orgName/receivers/$receiverName"
                    }
                }
            }
        }
    }
}

/**
 * Handle a setting for a single entity. Includes the run method.
 */
abstract class SingleSettingCommandNoSettingName(
    val name: String,
    val help: String,
    val settingType: SettingType,
    val operation: Operation
) : SettingCommand(name = name, help = help) {
    open val settingName: String = ""

    val useJson by option(
        "--json",
        help = "Use the JSON format instead of YAML"
    ).flag(default = false)

    private val verbose by option(
        "--verbose",
        help = "Verbose output"
    ).flag(default = false)

    override fun run() {
        // Authenticate
        val environment = getEnvironment()
        val accessToken = getAccessToken(environment)

        // Operations
        when (operation) {
            Operation.LIST -> {
                if (settingType != SettingType.ORG && settingName.isBlank())
                    abort("Missing organization name argument")
                val output = listNames(environment, accessToken, settingType, settingName)
                writeOutput(output.joinToString("\n"))
            }
            Operation.GET -> {
                val output = get(environment, accessToken, settingType, settingName)
                if (useJson) writeOutput(output) else writeOutput(toYaml(output, settingType))
            }
            Operation.PUT -> {
                val (name, payload) = if (useJson)
                    fromJson(readInput(), settingType)
                else
                    fromYaml(readInput(), settingType)
                val output = put(environment, accessToken, settingType, name, payload)

                if (verbose) {
                    println("put ${settingType.toString().lowercase()} :: $payload")
                }

                writeOutput(output)
            }
            Operation.DELETE -> {
                delete(environment, accessToken, settingType, settingName)
                writeOutput("Removed $settingName")
            }
        }
    }
}

abstract class SingleSettingCommand(
    name: String,
    help: String,
    settingType: SettingType,
    operation: Operation
) : SingleSettingCommandNoSettingName(name, help, settingType, operation) {
    override val settingName: String by option(
        "-n", "--name",
        metavar = "<name>",
        help = "The full name of the setting"
    ).required()
}

abstract class SingleSettingWithInputCommand(
    name: String,
    help: String,
    settingType: SettingType,
    operation: Operation
) : SingleSettingCommandNoSettingName(name, help, settingType, operation) {
    override val inStream by option("-i", "--input", help = "Input from file", metavar = "<file>")
        .inputStream()
}

/**
 * Organization setting commands
 */
class OrganizationSettings : CliktCommand(
    name = "organization",
    help = "Fetch and update settings for an organization"
) {
    init {
        subcommands(
            ListOrganizationSetting(), GetOrganizationSetting(), PutOrganizationSetting(), DeleteOrganizationSetting()
        )
    }

    override fun run() {}
}

class ListOrganizationSetting : SingleSettingCommandNoSettingName(
    name = "list",
    help = "List the names of all organizations",
    settingType = SettingType.ORG,
    operation = Operation.LIST
)

class GetOrganizationSetting : SingleSettingCommand(
    name = "get",
    help = "Fetch a organization",
    settingType = SettingType.ORG,
    operation = Operation.GET
)

class PutOrganizationSetting : SingleSettingWithInputCommand(
    name = "set",
    help = "Update a organization",
    settingType = SettingType.ORG,
    operation = Operation.PUT
)

class DeleteOrganizationSetting : SingleSettingCommand(
    name = "remove",
    help = "Remove a organization",
    settingType = SettingType.ORG,
    operation = Operation.DELETE
)

/**
 * Sender setting commands
 */
class SenderSettings : CliktCommand(
    name = "sender",
    help = "Fetch and update settings for a sender"
) {
    init {
        subcommands(
            ListSenderSetting(),
            GetSenderSetting(),
            PutSenderSetting(),
            DeleteSenderSetting(),
            TokenUrl(),
            AddPublicKey(),
        ) }

    override fun run() {}
}

class ListSenderSetting : SingleSettingCommand(
    name = "list",
    help = "List all sender names for an organization",
    settingType = SettingType.SENDER,
    operation = Operation.LIST
)

class GetSenderSetting : SingleSettingCommand(
    name = "get",
    help = "Fetch a sender",
    settingType = SettingType.SENDER,
    operation = Operation.GET
)

class PutSenderSetting : SingleSettingWithInputCommand(
    name = "set",
    help = "Update a sender",
    settingType = SettingType.SENDER,
    operation = Operation.PUT
)

class DeleteSenderSetting : SingleSettingCommand(
    name = "remove",
    help = "Remove a sender",
    settingType = SettingType.SENDER,
    operation = Operation.DELETE
)

/**
 * Receiver setting commands
 */
class ReceiverSettings : CliktCommand(
    name = "receiver",
    help = "Fetch and update settings for a receiver"
) {
    init { subcommands(ListReceiverSetting(), GetReceiverSetting(), PutReceiverSetting(), DeleteReceiverSetting()) }

    override fun run() {}
}

class ListReceiverSetting : SingleSettingCommand(
    name = "list",
    help = "Fetch receiver names for an organization",
    settingType = SettingType.RECEIVER,
    operation = Operation.LIST
)

class GetReceiverSetting : SingleSettingCommand(
    name = "get",
    help = "Fetch a receiver",
    settingType = SettingType.RECEIVER,
    operation = Operation.GET
)

class PutReceiverSetting : SingleSettingWithInputCommand(
    name = "set",
    help = "Update a receiver",
    settingType = SettingType.RECEIVER,
    operation = Operation.PUT
)

class DeleteReceiverSetting : SingleSettingCommand(
    name = "remove",
    help = "Remove a receiver",
    settingType = SettingType.RECEIVER,
    operation = Operation.DELETE
)

/**
 * Update multiple settings
 */
class MultipleSettings : CliktCommand(
    name = "multiple-settings",
    help = "Retrieve and alter multiple settings using a 'organizations.yml' file."
) {
    init { subcommands(PutMultipleSettings(), GetMultipleSettings()) }

    override fun run() {}
}

class PutMultipleSettings : SettingCommand(
    name = "set",
    help = "set all settings from a 'organizations.yml' file"
) {

    override val inStream by option("-i", "--input", help = "Input from file", metavar = "<file>")
        .inputStream()

    private val verbose by option(
        "--verbose",
        help = "Verbose output"
    ).flag(default = false)

    override fun run() {
        val environment = getEnvironment()
        val accessToken = getAccessToken(environment)
        val results = putAll(environment, accessToken)
        val output = "${results.joinToString("\n")}\n"
        writeOutput(output)
    }

    fun putAll(environment: Environment, accessToken: String): List<String> {
        val deepOrgs = readYaml()
        val results = mutableListOf<String>()
        // Put orgs
        deepOrgs.forEach { deepOrg ->
            val org = Organization(deepOrg)
            val payload = jsonMapper.writeValueAsString(org)

            if (verbose) {
                println("""Organization :: $payload""")
            }

            results += put(environment, accessToken, SettingType.ORG, deepOrg.name, payload)
        }
        // Put senders
        deepOrgs.flatMap { it.senders }.forEach { sender ->
            val payload = jsonMapper.writeValueAsString(sender)

            if (verbose) {
                println("""Sender :: $payload""")
            }

            results += put(environment, accessToken, SettingType.SENDER, sender.fullName, payload)
        }
        // Put receivers
        deepOrgs.flatMap { it.receivers }.forEach { receiver ->
            val payload = jsonMapper.writeValueAsString(receiver)

            if (verbose) {
                println("""Receiver :: $payload""")
            }

            results += put(environment, accessToken, SettingType.RECEIVER, receiver.fullName, payload)
        }
        return results
    }

    fun readYaml(): List<DeepOrganization> {
        val input = readInput()
        return yamlMapper.readValue(input)
    }
}

class GetMultipleSettings : SettingCommand(
    name = "get",
    help = "get all settings from an environment in yaml format"
) {
    val filter by option(
        "-f", "--filter",
        help = "filter the organizations, only returning those with names that start with <filter>",
        metavar = "<filter>"
    )

    override fun run() {
        val environment = getEnvironment()
        val accessToken = getAccessToken(environment)
        val output = getAll(environment, accessToken)
        writeOutput(output)
    }

    fun getAll(environment: Environment, accessToken: String): String {
        // get orgs
        val orgsJson = getMany(environment, accessToken, SettingType.ORG, settingName = "")
        var orgs = jsonMapper.readValue(orgsJson, Array<OrganizationAPI>::class.java)
        if (filter != null) {
            orgs = orgs.filter { it.name.startsWith(filter!!, ignoreCase = true) }.toTypedArray()
        }

        // get senders and receivers per org
        val deepOrgs = orgs.map { org ->
            val sendersJson = getMany(environment, accessToken, SettingType.SENDER, org.name)
            val orgSenders = jsonMapper.readValue(sendersJson, Array<SenderAPI>::class.java).map { Sender(it) }
            val receiversJson = getMany(environment, accessToken, SettingType.RECEIVER, org.name)
            val orgReceivers = jsonMapper.readValue(receiversJson, Array<ReceiverAPI>::class.java).map { Receiver(it) }
            DeepOrganization(org, orgSenders, orgReceivers)
        }
        return yamlMapper.writeValueAsString(deepOrgs)
    }
}