package gov.cdc.prime.router.secrets

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException
import kotlin.test.fail

internal class SecretServiceTests : SecretManagement {

    private val mockSecretService = spyk(EnvVarSecretService)

    override val secretService: SecretService
        get() = mockSecretService

    // @Test
    fun `test fetch from envVar`() {
        every { mockSecretService.fetchEnvironmentVariable("SECRET_SERVICE_TEST") } returns "value_expected"
        assertThat("value_expected").isEqualTo(secretService.fetchSecret("SECRET_SERVICE_TEST"))
    }

    @Test
    fun `test fetchSecret handles valid secretNames`() {
        VALID_SECRET_NAMES.forEach {
            secretService.fetchSecret(it)
        }
    }

    @Test
    fun `test fetchSecret throws IllegalArgumentException with non-url safe secretNames`() {
        INVALID_SECRET_NAMES.forEach {
            try {
                secretService.fetchSecret(it)
                fail("IllegalArgumentException not thrown for $it")
            } catch (e: IllegalArgumentException) {
                assertThat("secretName must match: ^[a-zA-Z0-9-_]*\$").isEqualTo(e.message)
            }
        }
    }

    companion object {
        private val VALID_SECRET_NAMES = listOf(
            "valid1", "35wtfsdfe4t4wr4w4343", "with-dashes-in-name-", "with_UNDER-score"
        )
        private val INVALID_SECRET_NAMES = listOf(
            "slashes/are/not/allowed", "no spaces", "?andotherthings"
        )
    }
}