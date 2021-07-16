package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.db.enums.SettingType
import gov.cdc.prime.router.azure.db.tables.pojos.Setting
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.spyk
import org.jooq.JSONB
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.OffsetDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingFacadeTests {
    private val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    private val connection = MockConnection(dataProvider)
    private val accessSpy = spyk(DatabaseAccess(connection))

    private val testOrg = Setting(
        1,
        SettingType.ORGANIZATION,
        "test",
        null,
        JSONB.valueOf(
            """{"name":"test","description":"Arizona PHD","jurisdiction":""" +
                """"STATE","stateCode":"CA","countyName":null,"meta":null}")"""
        ),
        false,
        true,
        1,
        "todo",
        OffsetDateTime.now()
    )
    private val defaultSender = Setting(
        2,
        SettingType.SENDER,
        "test",
        1,
        JSONB.valueOf(
            """{"name":"default","organizationName":"test","format":"CSV","topic":"covid-19"""" +
                ""","schemaName":"primedatainput/pdi-covid-19","meta":null}"""
        ),
        false,
        true,
        4,
        "todo",
        OffsetDateTime.now()
    )
    private val elrReceiver = Setting(
        3,
        SettingType.RECEIVER,
        "elr-test",
        1,
        JSONB.valueOf(
            """{"name":"elr-test","organizationName":"test","topic":"covid-19","translation":""" +
                """{"schemaName":"az/az-covid-19","format":"CSV","defaults":{},"type":"CUSTOM"},""" +
                """"jurisdictionalFilter":["matches(ordering_facility_state,AZ)",""" +
                """"doesNotMatch(ordering_facility_name,Tucson Mountains,Tucson Foothills,Sierra Vista Canyons)"]""" +
                ""","deidentify":false,"timing":{"operation":"MERGE","numberPerDay":1440,""" +
                """"initialTime":"00:00","timeZone":"ARIZONA","maxReportCount":100},"description":"","transport":""" +
                """{"host":"sftp","port":"22","filePath":"./upload","type":"SFTP"},"meta":null}"""
        ),
        false,
        true,
        5,
        "todo",
        OffsetDateTime.now()
    )

    private fun setupOrgDatabaseAccess() {
        every {
            accessSpy.fetchSetting(SettingType.ORGANIZATION, "test", null, any())
        }.returns(testOrg)
        every {
            accessSpy.fetchSetting(SettingType.ORGANIZATION, not("test"), parentId = null, any())
        }.returns(null)
        every {
            accessSpy.fetchSettings(SettingType.ORGANIZATION, txn = any())
        }.returns(listOf(testOrg))

        every {
            accessSpy.fetchSettings(SettingType.SENDER, txn = any())
        }.returns(listOf(defaultSender))
    }

    private fun setupSenderDatabaseAccess() {
        setupOrgDatabaseAccess()
        every {
            accessSpy.fetchSetting(SettingType.SENDER, "default", "test", any())
        }.returns(defaultSender)
        every {
            accessSpy.fetchSetting(SettingType.SENDER, "default", 1, any())
        }.returns(defaultSender)
        every {
            accessSpy.fetchSetting(SettingType.SENDER, not("default"), parentId = any(), any())
        }.returns(null)
        every {
            accessSpy.fetchSetting(SettingType.SENDER, not("default"), organizationName = any(), any())
        }.returns(null)
    }

    private fun setupReceiverDatabaseAccess() {
        setupOrgDatabaseAccess()
        every {
            accessSpy.fetchSettings(SettingType.RECEIVER, txn = any())
        }.returns(listOf(elrReceiver))
        every {
            accessSpy.fetchSetting(SettingType.RECEIVER, "elr-test", "test", any())
        }.returns(elrReceiver)
        every {
            accessSpy.fetchSetting(SettingType.RECEIVER, "elr-test", 1, any())
        }.returns(elrReceiver)
        every {
            accessSpy.fetchSetting(SettingType.RECEIVER, not("elr-test"), parentId = any(), any())
        }.returns(null)
        every {
            accessSpy.fetchSetting(SettingType.RECEIVER, not("elr-test"), organizationName = any(), any())
        }.returns(null)
    }

    private fun testMetadata(): Metadata {
        return Metadata(
            schema = Schema("primedatainput/pdi-covid-19", "covid-19")
        )
    }

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `get orgs test`() {
        setupOrgDatabaseAccess()
        val list = SettingsFacade(testMetadata(), accessSpy).organizations
        assertThat("test").isEqualTo(list.first().name)
        assertThat(Organization.Jurisdiction.STATE).isEqualTo(list.first().jurisdiction)
    }

    @Test
    fun `get org test`() {
        setupOrgDatabaseAccess()
        val org = SettingsFacade(testMetadata(), accessSpy).findOrganization("test")
        assertThat("test").isEqualTo(org?.name)
        assertThat(Organization.Jurisdiction.STATE).isEqualTo(org?.jurisdiction)
    }

    @Test
    fun `get org test failure`() {
        setupOrgDatabaseAccess()
        val org = SettingsFacade(testMetadata(), accessSpy).findOrganization("foo")
        assertThat(org).isNull()
    }

    @Test
    fun `get senders test`() {
        setupSenderDatabaseAccess()
        val list = SettingsFacade(testMetadata(), accessSpy).senders
        assertThat("default").isEqualTo(list.first().name)
        assertThat(Sender.Format.CSV).isEqualTo(list.first().format)
    }

    @Test
    fun `get sender test`() {
        setupSenderDatabaseAccess()
        val sender = SettingsFacade(testMetadata(), accessSpy).findSender("test.default")
        assertThat("default").isEqualTo(sender?.name)
        assertThat(Sender.Format.CSV).isEqualTo(sender?.format)
    }

    @Test
    fun `get sender test failure`() {
        setupSenderDatabaseAccess()
        val sender = SettingsFacade(testMetadata(), accessSpy).findSender("test.foo")
        assertThat(sender).isNull()
    }

    @Test
    fun `get sender test failure2`() {
        setupSenderDatabaseAccess()
        val sender = SettingsFacade(testMetadata(), accessSpy).findSender("foo.foo")
        assertThat(sender).isNull()
    }

    @Test
    fun `get receivers test`() {
        setupReceiverDatabaseAccess()
        val list = SettingsFacade(testMetadata(), accessSpy).receivers
        // assertEquals("elr-test", list.first().name)
        assertThat("elr-test").isEqualTo(list.first().name)
        // assertEquals(Report.Format.CSV, list.first().format)
        assertThat(Report.Format.CSV).isEqualTo(list.first().format)
    }

    @Test
    fun `get receiver test`() {
        setupReceiverDatabaseAccess()
        val receiver = SettingsFacade(testMetadata(), accessSpy).findReceiver("test.elr-test")
        // assertEquals("elr-test", receiver?.name)
        assertThat("elr-test").isEqualTo(receiver?.name)
        // assertEquals(Report.Format.CSV, receiver?.format)
        assertThat(Report.Format.CSV).isEqualTo(receiver?.format)
    }
}