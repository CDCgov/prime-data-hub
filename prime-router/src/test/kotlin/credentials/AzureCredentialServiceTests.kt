package gov.cdc.prime.router.credentials

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.azure.core.credential.TokenCredential
import com.azure.security.keyvault.secrets.SecretClient
import com.azure.security.keyvault.secrets.SecretClientBuilder
import com.azure.security.keyvault.secrets.models.KeyVaultSecret
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail

internal class AzureCredentialServiceTests {

    private val credentialService = spyk(AzureCredentialService, recordPrivateCalls = true)
    private val secretClient = mockk<SecretClient>()

    @BeforeTest
    fun setUp() {
        every { credentialService getProperty "KEY_VAULT_NAME" } returns KEY_VAULT_NAME
        every { credentialService getProperty "secretClient" } returns secretClient
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(credentialService)
    }

    @Test
    fun `initializes a Key Vault using the correct URL`() {
        val secretClientBuilder = mockkClass(SecretClientBuilder::class)
        every { secretClientBuilder.vaultUrl(any()) } returns secretClientBuilder
        every { secretClientBuilder.credential(any()) } returns secretClientBuilder
        every { secretClientBuilder.retryPolicy(any()) } returns secretClientBuilder
        every { secretClientBuilder.buildClient() } returns secretClient

        val mockAzureCredential = mockkClass(TokenCredential::class)

        val retVal = credentialService.initSecretClient(
            secretClientBuilder = secretClientBuilder, credential = mockAzureCredential
        )
        verify { secretClientBuilder.vaultUrl("https://$KEY_VAULT_NAME.vault.azure.net") }
        verify { secretClientBuilder.credential(mockAzureCredential) }

        assertThat(secretClient).isEqualTo(retVal)
    }

    @Test
    fun `uses Key Vault to retrieve a secret`() {
        every { secretClient.getSecret(any()) } returns KeyVaultSecret("$CONNECTION_ID", VALID_CREDENTIAL_JSON)
        val credential = credentialService.fetchCredential(
            CONNECTION_ID, CALLER_ID, CredentialRequestReason.AUTOMATED_TEST
        )
        assertThat(VALID_CREDENTIAL).isEqualTo(credential)
    }

    @Test
    fun `uses Key Vault to persist a secret`() {
        every { secretClient.setSecret(any(), any()) } returns mockkClass(KeyVaultSecret::class)
        credentialService.saveCredential(CONNECTION_ID, VALID_CREDENTIAL, CALLER_ID)
        verify { secretClient.setSecret("$CONNECTION_ID", VALID_CREDENTIAL_JSON) }
    }

    @Test
    fun `Key Vault errors when secret not persisted`() {
        every { secretClient.setSecret(any(), any()) } returns null
        try {
            credentialService.saveCredential(CONNECTION_ID, VALID_CREDENTIAL, CALLER_ID)
            fail("Expected Exception when no secret is returned")
        } catch (e: Exception) {
            assertThat("Failed to save credentials for: $CONNECTION_ID").isEqualTo(e.message)
        }
    }

    companion object {
        private const val CALLER_ID = "AzureCredentialServiceTests"
        private const val KEY_VAULT_NAME = "UNIT_TEST_KEY_VAULT"
        private const val CONNECTION_ID = "connection-id-1"
        private val VALID_CREDENTIAL = UserPassCredential("user1", "pass1")
        private const val VALID_CREDENTIAL_JSON = """{"@type":"UserPass","user":"user1","pass":"pass1"}"""
    }
}