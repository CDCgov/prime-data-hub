package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class AuthenticationTests {
    private val verifier = OktaAuthenticationVerifier()

    @Test
    fun `test user level checkMembership`() {
        val userMemberships = listOf("DHpima_az_phd")
        assertThat(
            verifier.checkMembership(userMemberships, PrincipalLevel.USER, organizationName = "pima-az-phd")
        ).isTrue()
        assertThat(
            verifier.checkMembership(userMemberships, PrincipalLevel.USER, organizationName = "az-phd")
        ).isFalse()
    }

    @Test
    fun `test multiple user level checkMembership`() {
        val userMemberships = listOf("DHpima_az_phd", "DHaz_phd")
        assertThat(
            (verifier.checkMembership(userMemberships, PrincipalLevel.USER, organizationName = "pima-az-phd"))
        ).isTrue()
        assertThat(
            verifier.checkMembership(userMemberships, PrincipalLevel.USER, organizationName = "az-phd")
        ).isTrue()
        assertThat(
            verifier.checkMembership(userMemberships, PrincipalLevel.USER, organizationName = "pa-phd")
        ).isFalse()
        assertThat(verifier.checkMembership(userMemberships, PrincipalLevel.USER, organizationName = null)).isFalse()
    }

    @Test
    fun `test user level checkMembership with admin account`() {
        val userMemberships = listOf("DHpima_az_phdAdmins", "DHfoo_ax")
        assertThat(
            verifier.checkMembership(userMemberships, PrincipalLevel.USER, "pima-az-phd")
        ).isTrue()
        assertThat(verifier.checkMembership(userMemberships, PrincipalLevel.USER, "foo")).isFalse()
    }

    @Test
    fun `test user level checkMembership with system account`() {
        val userMemberships = listOf("DHPrimeAdmins")
        assertThat(
            verifier.checkMembership(userMemberships, PrincipalLevel.USER, "pima-az-phd")
        ).isTrue()
        // Any org will return true
        assertThat(verifier.checkMembership(userMemberships, PrincipalLevel.USER, "foo")).isTrue()
        val multiMemberships = listOf("DHPrimeAdmins", "DHfox")
        assertThat(verifier.checkMembership(multiMemberships, PrincipalLevel.USER, "foo")).isTrue()
    }

    @Test
    fun `test admin level checkMembership with system account`() {
        // A Prime Admin should always have verified
        val single = listOf("DHPrimeAdmins")
        assertThat(
            verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, "pima-az-phd")
        ).isTrue()
        assertThat(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, "foo")).isTrue()
        assertThat(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, null)).isTrue()
        val multi = listOf("DHPrimeAdmins", "DHfox")
        assertThat(verifier.checkMembership(multi, PrincipalLevel.ORGANIZATION_ADMIN, "foo")).isTrue()
    }

    @Test
    fun `test admin level checkMembership with user account`() {
        // A Prime Admin should always have verified
        val single = listOf("DHaz_phd")
        assertThat(
            verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, "az-phd")
        ).isFalse()
        assertThat(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, "foo")).isFalse()
        assertThat(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, null)).isFalse()
        val multi = listOf("DHaz_phd", "DHfox")
        assertThat(verifier.checkMembership(multi, PrincipalLevel.ORGANIZATION_ADMIN, "foo")).isFalse()
    }

    @Test
    fun `test admin level checkMembership`() {
        // a pima_az_phd admin should only be valid for pima-az-orgs
        val single = listOf("DHpima_az_phdAdmins")
        assertThat(
            verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, "pima-az-phd")
        ).isTrue()
        assertThat(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, "foo")).isFalse()
        assertThat(verifier.checkMembership(single, PrincipalLevel.ORGANIZATION_ADMIN, null)).isFalse()
        val multi = listOf("DHpima_az_phd", "DHpima_az_phdAdmins")
        assertThat(
            verifier.checkMembership(multi, PrincipalLevel.ORGANIZATION_ADMIN, "pima-az-phd")
        ).isTrue()
        assertThat(verifier.checkMembership(multi, PrincipalLevel.ORGANIZATION_ADMIN, "foo")).isFalse()
        assertThat(verifier.checkMembership(multi, PrincipalLevel.ORGANIZATION_ADMIN, null)).isFalse()
    }

    @Test
    fun `test system level checkMembership with system account`() {
        val userMemberships = listOf("DHPrimeAdmins")
        assertThat(
            verifier.checkMembership(userMemberships, PrincipalLevel.SYSTEM_ADMIN, "foo")
        ).isTrue()
        assertThat(verifier.checkMembership(userMemberships, PrincipalLevel.SYSTEM_ADMIN, null)).isTrue()
        val multiMemberships = listOf("DHPrimeAdmins", "DHfox")
        assertThat(
            verifier.checkMembership(multiMemberships, PrincipalLevel.SYSTEM_ADMIN, "foo")
        ).isTrue()
    }
}