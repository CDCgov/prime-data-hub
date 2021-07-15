package gov.cdc.prime.router.serializers.datatests

import assertk.assertThat
import assertk.assertions.isTrue
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.TestSource
import gov.cdc.prime.router.Translator
import gov.cdc.prime.router.cli.tests.CompareData
import gov.cdc.prime.router.serializers.CsvSerializer
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.serializers.ReadResult
import net.jcip.annotations.NotThreadSafe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.function.Executable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.TimeZone
import kotlin.test.assertTrue
import kotlin.test.fail

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// Makes this not thread safe to keep other test classes being affected by the timezone change.
@NotThreadSafe
class TranslationTests {
    /**
     * The folder in the resources folder where the configuration and test data reside.
     */
    private val testDataDir = "/datatests"

    /**
     * The path to the configuration file from the test data directory.
     */
    private val testConfigFile = "conversion-test-config.csv"

    /**
     * The metadata
     */
    private val metadata = Metadata("./metadata")

    /**
     * The translator
     */
    private val translator = Translator(metadata, FileSettings(FileSettings.defaultSettingsDirectory))

    /**
     * The original timezone of the JVM
     */
    private val origDefaultTimeZone = TimeZone.getDefault()

    /**
     * The fields in the test configuration file
     */
    enum class ConfigColumns(val colName: String) {
        /**
         * The input file.
         */
        INPUT_FILE("Input File"),

        /**
         * The expected output file.
         */
        EXPECTED_FILE("Expected File"),

        /**
         * The name of the sender to use including the organization and client name.
         */
        INPUT_SCHEMA("Input schema"),

        /**
         * The organization name for the receiver.
         */
        OUTPUT_SCHEMA("Output schema"),

        /**
         * The organization name for the receiver.
         */
        OUTPUT_FORMAT("Output format")
    }

    data class TestConfig(
        val inputFile: String,
        val inputFormat: Report.Format,
        val inputSchema: Schema,
        val expectedFile: String,
        val expectedFormat: Report.Format,
        val expectedSchema: Schema
    )

    /**
     * Set the default timezone to GMT to match the build and deployment environments, so dates compare correctly
     * regarless of environment.
     */
    @BeforeAll
    fun setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT0"))
    }

    /**
     * Reset the timezone back to the original
     */
    @AfterAll
    fun resetDefaultTimezone() {
        TimeZone.setDefault(origDefaultTimeZone)
    }

    /**
     * Generate individual unit tests for each test file in the test folder.
     * @return a list of dynamic unit tests
     */
    @TestFactory
    fun generateDataTests(): Collection<DynamicTest> {
        val config = readTestConfig("$testDataDir/$testConfigFile")
        return config.map {
            DynamicTest.dynamicTest("Test ${it.inputFile}", FileConversionTest(it))
        }
    }

    /**
     * Read the configuration file [configPathname] for this test.
     * @return a list of tests to perform
     */
    private fun readTestConfig(configPathname: String): List<TestConfig> {
        var config = emptyList<TestConfig>()
        // Note we can only use input streams since the file may be in a JAR
        val resourceStream = this::class.java.getResourceAsStream(configPathname)
        if (resourceStream != null) {
            config = csvReader().readAllWithHeader(resourceStream).map {
                // Make sure we have all the fields we need.
                if (!it[ConfigColumns.INPUT_FILE.colName].isNullOrBlank() &&
                    !it[ConfigColumns.EXPECTED_FILE.colName].isNullOrBlank() &&
                    !it[ConfigColumns.INPUT_SCHEMA.colName].isNullOrBlank() &&
                    !it[ConfigColumns.OUTPUT_SCHEMA.colName].isNullOrBlank() &&
                    !it[ConfigColumns.OUTPUT_FORMAT.colName].isNullOrBlank()
                ) {
                    val inputSchema = metadata.findSchema(it[ConfigColumns.INPUT_SCHEMA.colName]!!)
                    val expectedSchema = metadata.findSchema(it[ConfigColumns.OUTPUT_SCHEMA.colName]!!)

                    if (inputSchema != null && expectedSchema != null) {
                        TestConfig(
                            it[ConfigColumns.INPUT_FILE.colName]!!,
                            getFormat(it[ConfigColumns.INPUT_FILE.colName]!!),
                            inputSchema,
                            it[ConfigColumns.EXPECTED_FILE.colName]!!,
                            Report.Format.safeValueOf(it[ConfigColumns.OUTPUT_FORMAT.colName]),
                            expectedSchema
                        )
                    } else if (inputSchema == null) {
                        fail("Input schema $inputSchema was not found.")
                    } else {
                        fail("Output schema $expectedSchema was not found.")
                    }
                } else {
                    fail("One or more config columns in $configPathname are empty.")
                }
            }
        }
        return config
    }

    /**
     * Get the report format from the extension of a [filename].
     * @return the report format
     */
    private fun getFormat(filename: String): Report.Format {
        return when {
            File(filename).extension.uppercase() == "INTERNAL" || filename.uppercase().endsWith("INTERNAL.CSV") -> {
                Report.Format.INTERNAL
            }
            File(filename).extension.uppercase() == "HL7" -> {
                Report.Format.HL7
            }
            else -> {
                Report.Format.CSV
            }
        }
    }

    /**
     * Perform test based on the given configuration.
     */
    inner class FileConversionTest(private val config: TestConfig) : Executable {
        override fun execute() {
            // First read in the data
            val inputFile = "$testDataDir/${config.inputFile}"
            val expectedFile = "$testDataDir/${config.expectedFile}"
            val inputStream = this::class.java.getResourceAsStream(inputFile)
            val expectedStream = this::class.java.getResourceAsStream(expectedFile)
            if (inputStream != null && expectedStream != null) {
                val inputReport = readReport(inputFile, inputStream, config.inputSchema, config.inputFormat)
                val translatedReport = translateReport(inputReport, config)
                val actualStream = outputReport(translatedReport, config.expectedFormat)
                val result = CompareData().compare(
                    expectedStream, actualStream, config.expectedFormat,
                    config.expectedSchema
                )
                assertTrue(
                    result.passed,
                    "${result.errors.joinToString("\n")}\n" +
                        result.warnings.joinToString("\n")
                )
            } else if (inputStream == null) {
                fail("The file ${config.inputFile} was not found.")
            } else {
                fail("The file ${config.expectedFile} was not found.")
            }
        }

        /**
         * Read the report from an [input] based on the provided [schema] and [format].
         * @return the report
         */
        private fun readReport(
            inputFile: String,
            input: InputStream,
            schema: Schema,
            format: Report.Format
        ): Report {

            return when (format) {
                Report.Format.HL7 -> {
                    val result = Hl7Serializer(metadata).readExternal(
                        schema.name,
                        input,
                        TestSource
                    )
                    checkReportResult(result, inputFile)
                    result.report!!
                }
                Report.Format.INTERNAL -> {
                    CsvSerializer(metadata).readInternal(
                        schema.name,
                        input,
                        listOf(TestSource),
                        useDefaultsForMissing = true
                    )
                }
                else -> {
                    val result = CsvSerializer(metadata).readExternal(
                        schema.name,
                        input,
                        TestSource
                    )
                    checkReportResult(result, inputFile)
                    result.report!!
                }
            }
        }

        /**
         * Outputs a [report] to the specified [format].
         * @return the report output
         */
        private fun outputReport(
            report: Report,
            format: Report.Format
        ): InputStream {
            val outputStream = ByteArrayOutputStream()
            when (format) {
                Report.Format.HL7_BATCH -> Hl7Serializer(metadata).writeBatch(report, outputStream)
                Report.Format.HL7 -> Hl7Serializer(metadata).write(report, outputStream)
                Report.Format.INTERNAL -> CsvSerializer(metadata).writeInternal(report, outputStream)
                else -> CsvSerializer(metadata).write(report, outputStream)
            }
            assertThat(outputStream.size() > 0).isTrue()

            return ByteArrayInputStream(outputStream.toByteArray())
        }

        /**
         * Checks the [result] of a report for errors.
         */
        private fun checkReportResult(result: ReadResult, filename: String) {
            assertTrue(
                result.errors.isEmpty(),
                "There were ${result.errors.size} errors while reading $filename with " +
                    " ${result.warnings.size} warning(s)\n" + result.errors.joinToString("\n") + "\n" +
                    result.warnings.joinToString("\n")
            )
            if (result.warnings.isNotEmpty()) println(result.warnings.joinToString("\n"))
        }

        /**
         * Translate an [report] based on the provided [config].
         */
        private fun translateReport(report: Report, config: TestConfig): Report {
            val mapping = translator.buildMapping(
                config.expectedSchema,
                config.inputSchema,
                emptyMap()
            )
            if (mapping.missing.isNotEmpty()) {
                fail(
                    "When translating to $'${config.expectedSchema.name} " +
                        "missing fields for ${mapping.missing.joinToString(", ")}"
                )
            }
            return report.applyMapping(mapping)
        }
    }
}