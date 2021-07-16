package gov.cdc.prime.router.transport

import assertk.assertThat
import assertk.assertions.* // ktlint-disable no-wildcard-imports
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.RedoxTransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.db.tables.pojos.Task
import gov.cdc.prime.router.secrets.SecretService
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.Test

class RedoxTransportTests {
    val context = mockkClass(ExecutionContext::class)
    val metadata = Metadata(Metadata.defaultMetadataDirectory)
    val settings = FileSettings(FileSettings.defaultSettingsDirectory)
    val logger = mockkClass(Logger::class)
    val reportId = UUID.randomUUID()
    val redox = spyk<RedoxTransport>()
    val actionHistory = ActionHistory(TaskAction.send, context)
    val secretService = mockk<SecretService>()
    val transportType = RedoxTransportType("", "baseURL")
    val task = Task(
        reportId,
        TaskAction.send,
        null,
        null,
        "az-phd.elr-test",
        4,
        "",
        "",
        null,
        null,
        null,
        null,
        null,
        null,
        null
    )
    private val reportFile = ReportFile(
        reportId,
        null,
        TaskAction.send,
        null,
        null,
        null,
        "az-phd",
        "elr-test",
        null, null, null, null, null, null, null, null,
        4, // pretend we have 4 items to send.
        null, null, null
    )

    fun setupLogger() {
        every { context.logger }.returns(logger)
        every { logger.log(any(), any(), any<Throwable>()) }.returns(Unit)
        every { logger.info(any<String>()) }.returns(Unit)
    }

    fun makeHeader(): WorkflowEngine.Header {
        val content = "Redox Message 0\nRedox Message 1\nRedox Message 2\nRedox Message 3"
        return WorkflowEngine.Header(
            task, reportFile,
            null,
            settings.findOrganization("ignore"),
            settings.findReceiver("ignore.REDOX"),
            metadata.findSchema("covid-19"),
            content = content.toByteArray(),
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `test send happy path`() {
        val header = makeHeader()
        setupLogger()
        every { redox.secretService }.returns(secretService)
        every { secretService.fetchSecret(any()) }.returns("dont_tell!")
        every { redox.fetchToken(any(), any(), any(), any()) }.returns("token")
        every { redox.sendItem(any(), any(), any(), any()) }
            .returns(RedoxTransport.SendResult("itemId1", RedoxTransport.ResultStatus.SUCCESS, 1234))
        // The Test
        val retryItems = redox.send(transportType, header, UUID.randomUUID(), null, context, actionHistory)

        assertThat(retryItems).isNull()
    }

    @Test
    fun `test send success on retry`() {
        val header = makeHeader()
        setupLogger()
        every { redox.secretService }.returns(secretService)
        every { secretService.fetchSecret(any()) }.returns("dont_tell!")
        every { redox.fetchToken(any(), any(), any(), any()) }.returns("token")
        every { redox.sendItem(any(), any(), any(), any()) }
            .returns(RedoxTransport.SendResult("itemId1", RedoxTransport.ResultStatus.SUCCESS, 1234))
        val retryItemsIn = listOf("0", "1", "2")
        // The Test
        val retryItemsOut = redox.send(transportType, header, UUID.randomUUID(), retryItemsIn, context, actionHistory)

        assertThat(retryItemsOut).isNull()
    }

    @Test
    fun `test all sendItem calls fail`() {
        val header = makeHeader()
        setupLogger()
        every { redox.secretService }.returns(secretService)
        every { secretService.fetchSecret(any()) }.returns("dont_tell!")
        every { redox.fetchToken(any(), any(), any(), any()) }.returns("token")
        every { redox.sendItem(any(), any(), any(), any()) }
            .returns(RedoxTransport.SendResult("itemId1", RedoxTransport.ResultStatus.FAILURE, 1234))
        val retryItemsIn = null
        // The Test
        val retryItemsOut = redox.send(transportType, header, UUID.randomUUID(), retryItemsIn, context, actionHistory)

        assertThat(retryItemsOut).isNotNull()
        if (retryItemsOut != null) {
            assertThat(4).isEqualTo(retryItemsOut.size)
            assertThat("0").isEqualTo(retryItemsOut[0])
            assertThat("1").isEqualTo(retryItemsOut[1])
            assertThat("2").isEqualTo(retryItemsOut[2])
            assertThat("3").isEqualTo(retryItemsOut[3])
        }
    }

    @Test
    fun `test fetchSecret failure`() {

        val header = makeHeader()
        setupLogger()
        every { redox.secretService }.returns(secretService)
        every { secretService.fetchSecret(any()) }.throws(Exception("x"))
        every { redox.fetchToken(any(), any(), any(), any()) }.returns("token")
        every { redox.sendItem(any(), any(), any(), any()) }
            .returns(RedoxTransport.SendResult("itemId1", RedoxTransport.ResultStatus.SUCCESS, 1234))

        // fetchSecret fails, not on a retry situation.

        val retryItemsOut = redox.send(transportType, header, UUID.randomUUID(), null, context, actionHistory)
        assertThat(retryItemsOut).isNotNull()
        if (retryItemsOut != null) {
            assertThat(1).isEqualTo(retryItemsOut.size)
        }
        assertThat(RetryToken.isAllItems(retryItemsOut)).isTrue()

        // Now what if fetchSecret fails in a retry situation

        val retryItemsIn = listOf("0", "3")
        val retryItemsOut2 = redox.send(transportType, header, UUID.randomUUID(), retryItemsIn, context, actionHistory)
        assertThat(retryItemsOut2).isNotNull()
        if (retryItemsOut2 != null) {
            assertThat(2).isEqualTo(retryItemsOut2.size)
            assertThat("0").isEqualTo(retryItemsOut2[0])
            assertThat("3").isEqualTo(retryItemsOut2[1])
        }
    }

    @Test
    fun `test fetchToken fails`() {
        val header = makeHeader()
        setupLogger()
        every { redox.secretService }.returns(secretService)
        every { secretService.fetchSecret(any()) }.returns("dont_tell!")
        every { redox.fetchToken(any(), any(), any(), any()) }.returns(null) // failure
        every { redox.sendItem(any(), any(), any(), any()) }
            .returns(RedoxTransport.SendResult("itemId1", RedoxTransport.ResultStatus.SUCCESS, 1234))

        // fetchToken fails, not on a retry situation.
        val retryItemsOut = redox.send(transportType, header, UUID.randomUUID(), null, context, actionHistory)
        assertThat(retryItemsOut).isNotNull()
        if (retryItemsOut != null) {
            assertThat(1).isEqualTo(retryItemsOut.size)
        }
        assertThat(RetryToken.isAllItems(retryItemsOut)).isTrue()

        // Now what if fetchToken fails in a retry situation
        val retryItemsIn = listOf("1", "2")
        val retryItemsOut2 = redox.send(transportType, header, UUID.randomUUID(), retryItemsIn, context, actionHistory)
        assertThat(retryItemsOut2).isNotNull()
        if (retryItemsOut2 != null) {
            assertThat(2).isEqualTo(retryItemsOut2.size)
            assertThat("1").isEqualTo(retryItemsOut2[0])
            assertThat("2").isEqualTo(retryItemsOut2[1])
        }
    }

    @Test
    fun `test partial sendItem failure`() {
        val header = makeHeader()
        setupLogger()
        every { redox.secretService }.returns(secretService)
        every { secretService.fetchSecret(any()) }.returns("dont_tell!")
        every { redox.fetchToken(any(), any(), any(), any()) }.returns("my token")
        // Item 1 fails, all others succeed
        every { redox.sendItem(any(), any(), any(), eq("$reportId-1")) }
            .returns(RedoxTransport.SendResult("itemId1", RedoxTransport.ResultStatus.FAILURE, 1234))
        every { redox.sendItem(any(), any(), any(), neq("$reportId-1")) }
            .returns(RedoxTransport.SendResult("itemId1", RedoxTransport.ResultStatus.SUCCESS, 1234))

        // This is a retry after a retry.
        val retryItemsIn = listOf("1", "2", "3")
        val retryItemsOut = redox.send(transportType, header, UUID.randomUUID(), retryItemsIn, context, actionHistory)
        assertThat(retryItemsOut).isNotNull()
        if (retryItemsOut != null) {
            println("inside test")
            assertThat(1).isEqualTo(retryItemsOut.size)
            assertThat("1").isEqualTo(retryItemsOut[0])
        }
    }

    @Test
    fun `test exception partway doesnt messup retry`() {
        val header = makeHeader()
        setupLogger()
        every { redox.secretService }.returns(secretService)
        every { secretService.fetchSecret(any()) }.returns("dont_tell!")
        every { redox.fetchToken(any(), any(), any(), any()) }.returns("my token")
        // Item 1 throws an exception, all others fail.
        every { redox.sendItem(any(), any(), any(), eq("$reportId-1")) }
            .throws(Exception("x"))
        every { redox.sendItem(any(), any(), any(), neq("$reportId-1")) }
            .returns(RedoxTransport.SendResult("itemId1", RedoxTransport.ResultStatus.FAILURE, 1234))

        // This is a retry after a retry.
        val retryItemsIn = listOf("0", "1", "2", "3")
        val retryItemsOut = redox.send(transportType, header, UUID.randomUUID(), retryItemsIn, context, actionHistory)
        assertThat(retryItemsOut).isNotNull()
        if (retryItemsOut != null) {
            assertThat(4).isEqualTo(retryItemsOut.size)
            assertThat("0").isEqualTo(retryItemsOut[0])
            assertThat("1").isEqualTo(retryItemsOut[1])
            assertThat("2").isEqualTo(retryItemsOut[2])
            assertThat("3").isEqualTo(retryItemsOut[3])
        }
    }
    @Test
    fun `test complex situation`() {
        val header = makeHeader()
        setupLogger()
        every { redox.secretService }.returns(secretService)
        every { secretService.fetchSecret(any()) }.returns("dont_tell!")
        every { redox.fetchToken(any(), any(), any(), any()) }.returns("my token")
        // Item 0 throws an exception, item 1 succeeds, item 3 fails.  item 2 is not re-tried.
        every { redox.sendItem(any(), any(), any(), eq("$reportId-0")) }
            .throws(Exception("x"))
        every { redox.sendItem(any(), any(), any(), eq("$reportId-1")) }
            .returns(RedoxTransport.SendResult("itemId1", RedoxTransport.ResultStatus.SUCCESS, 1234))
        every { redox.sendItem(any(), any(), any(), eq("$reportId-3")) }
            .returns(RedoxTransport.SendResult("itemId1", RedoxTransport.ResultStatus.FAILURE, 1234))

        // This is a retry after a retry, with a mix of responses.  Item 2 is not re-tried.
        val retryItemsIn = listOf("0", "1", "3")
        val retryItemsOut = redox.send(transportType, header, UUID.randomUUID(), retryItemsIn, context, actionHistory)
        assertThat(retryItemsOut).isNotNull()
        if (retryItemsOut != null) {
            assertThat(2).isEqualTo(retryItemsOut.size)
            assertThat("0").isEqualTo(retryItemsOut[0]) // exception
            assertThat("3").isEqualTo(retryItemsOut[1]) // returned failure code
        }
    }
}