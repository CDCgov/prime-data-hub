package gov.cdc.prime.router.credentials

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.test.Test

internal class CredentialTests {

    @Test
    fun `test polymorphic serialization of UserPassCredential`() {
        val credential = UserPassCredential("user", "pass")
        val expectedJson = "{\"@type\":\"UserPass\",\"user\":\"user\",\"pass\":\"pass\"}"
        assertThat(expectedJson).isEqualTo(credential.toJSON())
    }

    @Test
    fun `test polymorphic deserialization of UserPassCredential`() {
        val json = "{\"@type\":\"UserPass\",\"user\":\"user\",\"pass\":\"pass\"}"
        val credential = Credential.fromJSON(json)
        @Suppress("USELESS_IS_CHECK")
        assertThat(credential is UserPassCredential).isTrue() // Gives off warning message: instance is always 'true'
        // Needed to gain access to credential.user and credential.pass
        if (credential is UserPassCredential) {
            assertThat("user").isEqualTo(credential.user)
            assertThat("pass").isEqualTo(credential.pass)
        }
    }
}