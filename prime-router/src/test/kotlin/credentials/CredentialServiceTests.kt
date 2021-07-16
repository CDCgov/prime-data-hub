package gov.cdc.prime.router.credentials

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.lang.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.fail

internal class CredentialServiceTests : CredentialManagement {

    override val credentialService: CredentialService
        get() = MemoryCredentialService

    @Test
    fun `test save and fetch`() {
        val connectionId = "id1"
        credentialService.saveCredential(connectionId, VALID_CREDENTIAL, "CredentialServiceTest")

        val retVal = credentialService.fetchCredential(
            connectionId, "CredentialServiceTest", CredentialRequestReason.AUTOMATED_TEST
        )
        assertThat(VALID_CREDENTIAL).isEqualTo(retVal)
    }

    @Test
    fun `test fetchCredential handles valid connectionIds`() {
        VALID_CONNECTION_IDS.forEach {
            credentialService.fetchCredential(
                it, "CredentialServiceTest", CredentialRequestReason.AUTOMATED_TEST
            )
        }
    }

    @Test
    fun `test fetchCredential throws IllegalArgumentException with non-url safe connectionIds`() {
        INVALID_CONNECTION_IDS.forEach {
            try {
                credentialService.fetchCredential(
                    it, "CredentialServiceTest", CredentialRequestReason.AUTOMATED_TEST
                )
                fail("IllegalArgumentException not thrown for $it")
            } catch (e: IllegalArgumentException) {
                assertThat("connectionId must match: ^[a-zA-Z0-9-]*\$").isEqualTo(e.message)
            }
        }
    }

    @Test
    fun `test saveCredential handles valid connectionIds`() {
        VALID_CONNECTION_IDS.forEach {
            credentialService.saveCredential(it, VALID_CREDENTIAL, "CredentialServiceTest")
        }
    }

    @Test
    fun `test saveCredential throws IllegalArgumentException with non-url safe connectionIds`() {
        INVALID_CONNECTION_IDS.forEach {
            try {
                credentialService.saveCredential(it, VALID_CREDENTIAL, "CredentialServiceTest")
                fail("IllegalArgumentException not thrown for $it")
            } catch (e: IllegalArgumentException) {
                assertThat("connectionId must match: ^[a-zA-Z0-9-]*\$").isEqualTo(e.message)
            }
        }
    }

    companion object {
        private val VALID_CONNECTION_IDS = listOf(
            "valid1", "35wtfsdfe4t4wr4w4343", "with-dashes-in-name-"
        )
        private val INVALID_CONNECTION_IDS = listOf(
            "slashes/are/not/allowed", "no spaces", "?andotherthings", "underscore_not-allowed"
        )
        private val VALID_CREDENTIAL = UserPassCredential("user1", "pass1")
    }
}