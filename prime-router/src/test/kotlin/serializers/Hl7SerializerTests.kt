package gov.cdc.prime.router.serializers

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.model.v251.datatype.DR
import ca.uhn.hl7v2.model.v251.datatype.DT
import ca.uhn.hl7v2.model.v251.datatype.DTM
import ca.uhn.hl7v2.model.v251.datatype.TS
import ca.uhn.hl7v2.model.v251.datatype.XTN
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.FileSource
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Hl7SerializerTests {
    private val hl7TestFileDir = "./src/test/hl7_test_files/"
    private val hl7SchemaName = "hl7/hl7-ingest-covid-19"
    private val testReport: Report
    private val context = DefaultHapiContext()
    private val serializer: Hl7Serializer
    private val csvSerializer: CsvSerializer
    private val covid19Schema: Schema
    private val sampleHl7Message: String

    init {
        val metadata = Metadata("./metadata")
        val inputStream = File("./src/test/unit_test_files/fake-pdi-covid-19.csv").inputStream()
        covid19Schema = metadata.findSchema(hl7SchemaName) ?: fail("Could not find target schema")
        csvSerializer = CsvSerializer(metadata)
        serializer = Hl7Serializer(metadata)
        testReport = csvSerializer.readExternal("primedatainput/pdi-covid-19", inputStream, TestSource).report ?: fail()
        sampleHl7Message = """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||Buckridge^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071||^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85||94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.19.4.6&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
NTE|1|L|This is a comment|RE
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
SPM|1|||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600^202102090000-0600
NTE|1|L|This is a final comment|RE"""
    }

    @Test
    fun `Test write batch`() {
        val outputStream = ByteArrayOutputStream()
        serializer.writeBatch(testReport, outputStream)
        val output = outputStream.toString(StandardCharsets.UTF_8)
        assertNotNull(output)
    }

    @Test
    fun `test write a message`() {
        val output = serializer.createMessage(testReport, 0)
        assertNotNull(output)
    }

    @Test
    fun `test reading message from serializer`() {
        // arrange
        val mcf = CanonicalModelClassFactory("2.5.1")
        context.modelClassFactory = mcf
        val parser = context.pipeParser
        // act
        val reg = "[\r\n]".toRegex()
        val cleanedMessage = reg.replace(sampleHl7Message, "\r")
        val hapiMsg = parser.parse(cleanedMessage)
        val terser = Terser(hapiMsg)
        // these messages are of type ORU_R01, so we can cast to that
        // as well, and let's test that while we're here as well
        val oru = hapiMsg as ORU_R01
        // assert
        assertEquals(
            "CDC PRIME - Atlanta, Georgia (Dekalb)",
            terser.get("/MSH-3-1")
        )
        assertEquals(
            "2.16.840.1.114222.4.1.237821",
            terser.get("/MSH-3-2")
        )
        assertEquals("South Rodneychester", terser.get("/.PID-11-3"))
        // check the oru cast
        assertNotNull(oru)
        assertNotNull(oru.patienT_RESULT.patient)
        assertNotNull(oru.patienT_RESULT.patient.pid)
        println(oru.printStructure())
    }

    @Test
    fun `test converting hl7 into mapped list of values`() {
        val mappedMessage = serializer.convertMessageToMap(sampleHl7Message, covid19Schema)
        val mappedValues = mappedMessage.row
        println("\ntest converting hl7 into mapped list of values:\n")
        mappedValues.forEach {
            println("${it.key}: ${it.value.joinToString()}")
        }
        assertTrue(mappedValues.containsKey("patient_city"))
        assertEquals("South Rodneychester", mappedValues["patient_city"]?.get(0))
    }

    @Test
    fun `test reading HL7 message from file`() {
        val inputFile = "$hl7TestFileDir/single_message.hl7"
        val message = File(inputFile).readText()
        val mappedMessage = serializer.convertMessageToMap(message, covid19Schema)
        val mappedValues = mappedMessage.row
        mappedValues.forEach {
            println("${it.key}: ${it.value.joinToString()}")
        }
        assertTrue(mappedValues.containsKey("patient_city"))
        assertEquals("South Rodneychester", mappedValues["patient_city"]?.get(0))
    }

    @Test
    fun `test reading HL7 batch message from file`() {
        val inputFile = "$hl7TestFileDir/batch_message.hl7"
        val message = File(inputFile).readText()
        val mappedMessage = serializer.convertBatchMessagesToMap(message, covid19Schema)
        val mappedValues = mappedMessage.mappedRows
        println("\ntest reading HL7 batch message from file:\n")
        mappedValues.forEach {
            println("${it.key}: ${it.value.joinToString()}")
        }
        assertTrue(mappedValues.containsKey("patient_city"))
        val cities = mappedValues["patient_city"]?.toSet()
        assertEquals(setOf("North Taylor", "South Rodneychester"), cities)
        println("Errors:")
        mappedMessage.errors.forEach {
            println(it)
        }
        println("Warnings:")
        mappedMessage.warnings.forEach {
            println(it)
        }
    }

    @Test
    fun `test reading HL7 batch and creating report instance`() {
        val inputFile = "$hl7TestFileDir/batch_message.hl7"
        val message = File(inputFile)
        val source = FileSource(inputFile)
        val readResult = serializer.readExternal(hl7SchemaName, message.inputStream(), source)
        val report = readResult.report ?: fail("Report was null and should not be")
        assertEquals("South Rodneychester", report.getString(0, "patient_city"))
        assertEquals("North Taylor", report.getString(1, "patient_city"))
        assertTrue(report.itemCount == 2)
        val hospitalized = (0 until report.itemCount).map { report.getString(it, "hospitalized") }
        assertEquals(setOf(""), hospitalized.toSet())
    }

    @Test
    fun `test XTN phone decoding`() {
        val metadata = Metadata("./metadata")
        val serializer = Hl7Serializer(metadata)
        val mockTerser = mockk<Terser>()
        val mockSegment = mockk<Segment>()
        val emptyPhoneField = mockk<XTN>()
        val emailField = mockk<XTN>()
        val phoneField = mockk<XTN>()
        val deprecatedPhoneField = mockk<XTN>()
        val element = Element("phone", Element.Type.TELEPHONE, hl7Field = "PID-13")

        // Bad field value
        every { mockTerser.getSegment(any()) } returns null
        var phoneNumber = serializer.decodeHl7TelecomData(
            mockTerser, Element("phone", Element.Type.TELEPHONE),
            "PID-BLAH"
        )
        assertEquals("", phoneNumber)

        // Segment not found
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertEquals("", phoneNumber)

        // No phone number due to zero repetitions
        every { mockTerser.getSegment(any()) } returns mockSegment
        every { mockSegment.getField(any()) } returns emptyArray()
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertEquals("", phoneNumber)

        // No phone number
        every { mockSegment.getField(any()) } returns arrayOf(emptyPhoneField) // This is only to get the number of reps
        every { mockTerser.get(any()) } returns ""
        every { emptyPhoneField.areaCityCode.isEmpty } returns true
        every { emptyPhoneField.localNumber.isEmpty } returns true
        every { emptyPhoneField.telephoneNumber.isEmpty } returns true
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertEquals("", phoneNumber)

        // Multiple repetitions with no phone number
        every { mockSegment.getField(any()) } returns arrayOf(emptyPhoneField, emptyPhoneField, emptyPhoneField)
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertEquals("", phoneNumber)

        // Phone number in deprecated component
        every { deprecatedPhoneField.areaCityCode.isEmpty } returns true
        every { deprecatedPhoneField.localNumber.isEmpty } returns true
        every { deprecatedPhoneField.telephoneNumber.isEmpty } returns false
        every { deprecatedPhoneField.telephoneNumber.valueOrEmpty } returns "(555)555-5555"
        every { mockSegment.getField(any()) } returns arrayOf(deprecatedPhoneField)
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertEquals("5555555555:1:", phoneNumber)

        // Phone number in newer components.  Will ignore phone number in deprecated component
        every { mockSegment.getField(any()) } returns arrayOf(phoneField)
        every { phoneField.areaCityCode.isEmpty } returns false
        every { phoneField.localNumber.isEmpty } returns false
        every { phoneField.telephoneNumber.value } returns "(555)555-5555"
        every { phoneField.telecommunicationEquipmentType.isEmpty } returns false
        every { phoneField.telecommunicationEquipmentType.valueOrEmpty } returns "PH"
        every { phoneField.countryCode.value } returns "1"
        every { phoneField.areaCityCode.value } returns "666"
        every { phoneField.localNumber.value } returns "7777777"
        every { phoneField.extension.value } returns "9999"
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertEquals("6667777777:1:9999", phoneNumber)

        // No type assumed to be a phone number
        every { phoneField.telecommunicationEquipmentType.isEmpty } returns true
        every { phoneField.telecommunicationEquipmentType.valueOrEmpty } returns null
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertEquals("6667777777:1:9999", phoneNumber)

        // A Fax number is not used
        every { phoneField.telecommunicationEquipmentType.isEmpty } returns false
        every { phoneField.telecommunicationEquipmentType.valueOrEmpty } returns "FX"
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertEquals("", phoneNumber)

        // Test repetitions.  The first repetition for the XTN type can be empty when there is no primary phone number
        every { phoneField.telecommunicationEquipmentType.valueOrEmpty } returns "PH"
        every { emailField.areaCityCode.isEmpty } returns true
        every { emailField.localNumber.isEmpty } returns true
        every { emailField.telephoneNumber.isEmpty } returns true
        every { emailField.emailAddress.valueOrEmpty } returns "dummyemail@cdc.local"
        every { emailField.telecommunicationEquipmentType.isEmpty } returns false
        every { emailField.telecommunicationEquipmentType.valueOrEmpty } returns "Internet"
        every { emailField.telecommunicationUseCode.valueOrEmpty } returns "NET"
        every { mockSegment.getField(any()) } returns arrayOf(emptyPhoneField, emailField, phoneField)
        phoneNumber = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertEquals("6667777777:1:9999", phoneNumber)
    }

    @Test
    fun `test XTN email decoding`() {
        val metadata = Metadata("./metadata")
        val serializer = Hl7Serializer(metadata)
        val mockTerser = mockk<Terser>()
        val mockSegment = mockk<Segment>()
        val emailField = mockk<XTN>()
        val phoneField = mockk<XTN>()
        val element = Element("email", Element.Type.EMAIL, hl7Field = "PID-13")

        // Bad field value
        every { mockTerser.getSegment(any()) } returns null
        var email = serializer.decodeHl7TelecomData(
            mockTerser, Element("email", Element.Type.EMAIL),
            "PID-BLAH"
        )
        assertEquals("", email)

        // Segment not found
        email = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertEquals("", email)

        // No email number due to zero repetitions
        every { mockTerser.getSegment(any()) } returns mockSegment
        every { mockSegment.getField(any()) } returns emptyArray()
        email = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertEquals("", email)

        // No email
        every { mockSegment.getField(any()) } returns arrayOf(phoneField)
        every { phoneField.telecommunicationEquipmentType.isEmpty } returns false
        every { phoneField.telecommunicationEquipmentType.valueOrEmpty } returns "PH"
        email = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertEquals("", email)

        // Test repetitions.
        every { emailField.emailAddress.valueOrEmpty } returns "dummyemail@cdc.local"
        every { emailField.telecommunicationEquipmentType.isEmpty } returns false
        every { emailField.telecommunicationEquipmentType.valueOrEmpty } returns "Internet"
        every { mockSegment.getField(any()) } returns arrayOf(emailField, phoneField)
        email = serializer.decodeHl7TelecomData(mockTerser, element, element.hl7Field!!)
        assertEquals("dummyemail@cdc.local", email)
    }

    @Test
    fun `test date time decoding`() {
        val metadata = Metadata("./metadata")
        val serializer = Hl7Serializer(metadata)
        val mockTerser = mockk<Terser>()
        val mockSegment = mockk<Segment>()
        val mockTS = mockk<TS>()
        val mockDR = mockk<DR>()
        val mockDTM = mockk<DTM>()
        val now = OffsetDateTime.now()
        val nowAsDate = Date.from(now.toInstant())
        val dateTimeElement = Element("field", hl7Field = "OBX-14", type = Element.Type.DATETIME)
        val warnings = mutableListOf<String>()
        val dateFormatterWithTimeZone = DateTimeFormatter.ofPattern(Element.datetimePattern)
        val dateFormatterNoTimeZone = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

        // Segment not found
        every { mockTerser.getSegment(any()) } returns null
        var dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertEquals("", dateTime)

        // Bad field value
        every { mockTerser.getSegment(any()) } returns mockSegment
        dateTime = serializer.decodeHl7DateTime(
            mockTerser, Element("field", hl7Field = "OBX-Blah"),
            "OBX-Blah", warnings
        )
        assertEquals("", dateTime)

        // No field value
        every { mockSegment.getField(any(), any()) } returns null
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertEquals("", dateTime)

        // Field value is TS, but no time
        every { mockSegment.getField(any(), any()) } returns mockTS
        every { mockTS.time } returns null
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertEquals("", dateTime)

        // Field value is TS has a time
        every { mockTS.time } returns mockDTM
        every { mockTS.time.valueAsDate } returns nowAsDate
        every { mockTS.time.value } returns dateFormatterWithTimeZone.format(now)
        every { mockTS.time.gmtOffset } returns 0
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertEquals(dateFormatterWithTimeZone.format(now.withOffsetSameInstant(ZoneOffset.UTC)), dateTime)

        // Field value is TS has a time, but no GMT offset
        every { mockTS.time } returns mockDTM
        val cal = Calendar.getInstance()
        cal.time = nowAsDate
        every { mockTS.time.valueAsCalendar } returns cal
        every { mockTS.time.value } returns dateFormatterWithTimeZone.format(now)
        every { mockTS.time.gmtOffset } returns -99
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertEquals(dateFormatterWithTimeZone.format(now.withOffsetSameInstant(ZoneOffset.UTC)), dateTime)

        // Field value is DR, but no range
        every { mockSegment.getField(any(), any()) } returns mockDR
        every { mockDR.rangeStartDateTime } returns null
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertEquals("", dateTime)

        // Field value is DR has a range, but with no time
        every { mockDR.rangeStartDateTime } returns mockTS
        every { mockDR.rangeStartDateTime.time } returns null
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertEquals("", dateTime)

        // Field value is DR and has a time
        every { mockDR.rangeStartDateTime } returns mockTS
        every { mockDR.rangeStartDateTime.time } returns mockDTM
        every { mockDR.rangeStartDateTime.time.valueAsDate } returns nowAsDate
        every { mockDR.rangeStartDateTime.time.value } returns dateFormatterWithTimeZone.format(now)
        dateTime = serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertEquals(dateFormatterWithTimeZone.format(now.withOffsetSameInstant(ZoneOffset.UTC)), dateTime)

        // Generate a warning for not having the timezone offsets
        every { mockDR.rangeStartDateTime } returns mockTS
        every { mockDR.rangeStartDateTime.time } returns mockDTM
        every { mockDR.rangeStartDateTime.time.valueAsDate } returns nowAsDate
        every { mockDR.rangeStartDateTime.time.value } returns dateFormatterNoTimeZone.format(now)
        warnings.clear()
        serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
        assertTrue(warnings.size == 1)

        // Test a bit more the regex for the warning
        fun testForTimestampWarning(dateString: String, numExpectedWarnings: Int) {
            // Note the data type here does not affect how the raw string gets check for a warning
            every { mockDR.rangeStartDateTime.time.valueAsDate.toInstant() } returns Date().toInstant()
            every { mockDR.toString() } returns dateString
            every { mockSegment.getField(any(), any()) } returns mockDR
            warnings.clear()
            serializer.decodeHl7DateTime(mockTerser, dateTimeElement, dateTimeElement.hl7Field!!, warnings)
            assertEquals(warnings.size, numExpectedWarnings)
        }

        testForTimestampWarning("TS[202101011200]", 1)
        testForTimestampWarning("TS[202101011200.0000]", 1)
        testForTimestampWarning("TS[2021010112-0400]", 1)
        testForTimestampWarning("DR[202101011200.0000-4000]", 0)
        testForTimestampWarning("TS[202101011200.0000+4000]", 0)
        testForTimestampWarning("DR[202101011259+4000]", 0)
    }

    @Test
    fun `test date decoding`() {
        val mockTerser = mockk<Terser>()
        val mockSegment = mockk<Segment>()
        val mockDT = mockk<DT>()
        val dateElement = Element("field", hl7Field = "OBX-14", type = Element.Type.DATE)
        val dateFormatterDate = DateTimeFormatter.ofPattern(Element.datePattern)
        val warnings = mutableListOf<String>()

        every { mockTerser.getSegment(any()) } returns mockSegment

        // Field value is DT with a date
        val date = LocalDate.of(1995, 1, 1)
        val formattedDate = dateFormatterDate.format(date)
        every { mockDT.toString() } returns formattedDate
        every { mockDT.year } returns date.year
        every { mockDT.month } returns date.monthValue
        every { mockDT.day } returns date.dayOfMonth
        every { mockSegment.getField(any(), any()) } returns mockDT
        val dateTime = serializer.decodeHl7DateTime(mockTerser, dateElement, dateElement.hl7Field!!, warnings)
        assertEquals(formattedDate, dateTime)

        // Test a bit more the regex for the warning
        fun testForDateWarning(dateString: String, numExpectedWarnings: Int) {
            every { mockDT.toString() } returns dateString
            every { mockDT.year } returns 1995
            every { mockDT.month } returns 1
            every { mockDT.day } returns 1
            every { mockSegment.getField(any(), any()) } returns mockDT
            warnings.clear()
            serializer.decodeHl7DateTime(mockTerser, dateElement, dateElement.hl7Field!!, warnings)
            assertEquals(warnings.size, numExpectedWarnings)
        }

        testForDateWarning("DT[19950101]", 0)
        testForDateWarning("TS[19950101120101.0001+4000]", 0)
        testForDateWarning("DT[199501]", 1)
        testForDateWarning("DT[1995]", 1)
    }

    @Test
    fun `test reading message with international characters from serializer`() {
        // Sample UTF-8 taken from https://www.kermitproject.org/utf8.html as a byte array, so we are not
        // restricted by the encoding of this code file
        val greekString = String(
            byteArrayOf(-50, -100, -49, -128, -50, -65, -49, -127, -49, -114),
            Charsets.UTF_8
        )

        // Java strings are stored as UTF-16
        val intMessage = """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|||20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
SFT|Centers for Disease Control and Prevention|0.1-SNAPSHOT|PRIME Data Hub|0.1-SNAPSHOT||20210210
PID|1||2a14112c-ece1-4f82-915c-7b3a8d152eda^^^Avante at Ormond Beach^PI||$greekString^Kareem^Millie^^^^L||19580810|F||2106-3^White^HL70005^^^^2.5.1|688 Leighann Inlet^^South Rodneychester^TX^67071||^PRN^^roscoe.wilkinson@email.com^1^211^2240784|||||||||U^Unknown^HL70189||||||||N
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^FL^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBR|1|73a6e9bd-aaec-418e-813a-0ad33366ca85||94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN|||202102090000-0600|202102090000-0600||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI|^WPN^^^1^386^6825220|||||202102090000-0600|||F
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.19.4.6&ISO^^^^10D0876999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
NTE|1|L|This is a comment|RE
OBX|2|CWE|95418-0^Whether patient is employed in a healthcare setting^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||Y^Yes^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|4|CWE|95421-4^Resides in a congregate care setting^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
OBX|5|CWE|95419-8^Has symptoms related to condition of interest^LN^^^^2.69||N^No^HL70136||||||F|||202102090000-0600|||||||||||||||QST
SPM|1|||258500001^Nasopharyngeal swab^SCT||||71836000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||202102090000-0600^202102090000-0600
NTE|1|L|This is a final comment|RE"""

        // arrange
        val mcf = CanonicalModelClassFactory("2.5.1")
        context.modelClassFactory = mcf
        val parser = context.pipeParser
        // act
        val reg = "[\r\n]".toRegex()
        val cleanedMessage = reg.replace(intMessage, "\r")
        val hapiMsg = parser.parse(cleanedMessage)
        val terser = Terser(hapiMsg)
        // assert
        assertEquals(
            greekString,
            terser.get("/.PID-5-1")
        )
    }

    @Test
    fun `test terser spec generator`() {
        val metadata = Metadata("./metadata")
        val serializer = Hl7Serializer(metadata)
        assertEquals("/MSH-1-1", serializer.getTerserSpec("MSH-1-1"))
        assertEquals("/.PID-1", serializer.getTerserSpec("PID-1"))
        assertEquals("/.", serializer.getTerserSpec(""))
    }

    @Test
    fun `test setTelephoneComponents for patient`() {
        val metadata = Metadata("./metadata")
        val mockTerser = mockk<Terser>()
        val serializer = Hl7Serializer(metadata)
        every { mockTerser.set(any(), any()) } returns Unit
        every { mockTerser.get("/PATIENT_RESULT/PATIENT/PID-13(0)-2") } returns ""

        val patientPathSpec = serializer.formPathSpec("PID-13")
        val patientElement = Element("patient_phone_number", hl7Field = "PID-13", type = Element.Type.TELEPHONE)
        serializer.setTelephoneComponent(
            mockTerser,
            "5555555555:1:",
            patientPathSpec,
            patientElement,
            Hl7Configuration.PhoneNumberFormatting.ONLY_DIGITS_IN_COMPONENT_ONE
        )

        verify {
            mockTerser.set("/PATIENT_RESULT/PATIENT/PID-13(0)-1", "5555555555")
            mockTerser.set("/PATIENT_RESULT/PATIENT/PID-13(0)-2", "PRN")
            mockTerser.set("/PATIENT_RESULT/PATIENT/PID-13(0)-3", "PH")
            mockTerser.set("/PATIENT_RESULT/PATIENT/PID-13(0)-5", "1")
            mockTerser.set("/PATIENT_RESULT/PATIENT/PID-13(0)-6", "555")
            mockTerser.set("/PATIENT_RESULT/PATIENT/PID-13(0)-7", "5555555")
        }
    }

    @Test
    fun `test setTelephoneComponents for facility`() {
        val metadata = Metadata("./metadata")
        val mockTerser = mockk<Terser>()
        val serializer = Hl7Serializer(metadata)
        every { mockTerser.set(any(), any()) } returns Unit

        val facilityPathSpec = serializer.formPathSpec("ORC-23")
        val facilityElement = Element(
            "ordering_facility_phone_number", hl7Field = "ORC-23",
            type = Element.Type.TELEPHONE
        )
        serializer.setTelephoneComponent(
            mockTerser,
            "5555555555:1:3333",
            facilityPathSpec,
            facilityElement,
            Hl7Configuration.PhoneNumberFormatting.STANDARD
        )

        verify {
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-1", "(555)555-5555X3333")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-2", "WPN")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-3", "PH")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-5", "1")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-6", "555")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-7", "5555555")
            mockTerser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-23-8", "3333")
        }
    }
}