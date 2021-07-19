package gov.cdc.prime.router.cli.tests

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Type
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.util.Hl7InputStreamMessageIterator
import ca.uhn.hl7v2.util.Terser
import com.github.ajalt.clikt.output.TermUi
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.ReportStreamEnv
import gov.cdc.prime.router.azure.WorkflowEngine
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Uses test data provided via a configuration file, sends the data to the API, then checks the response,
 * lineage results and compares the actual data with expected data provided in the configuration file.  This
 * test is run via a configuration file that specifies what the input and expected reports are for the test.
 * compare-test-config.csv has the following columns:
 *   Input File,Sender,Expected File,Organization name,Receiver name,Expected count
 * Where:
 *   Input File = input HL7 or CSV file relative to the config file folder
 *   Sender = the name of the sender including organization (e.g. ignore.ignore-hl7)
 *   Expected File = the name of one of the expected output files relative to the config file folder
 *   Organization name = the name of the receiver organization (e.g. ignore)
 *   Receiver name = the name of the receiver (e.g. CSV)
 *   Expected count = The number of expected records sent as an integer
 * You can specify one or more expected files for a given input file by having multiple lines with the same input
 * file.  E.g.
 *   Input File,Sender,Expected File,Organization name,Receiver name,Expected count
 *   test-0001-input-covid-19.hl7,ignore.ignore-hl7,test-0001-az-covid-19-hl7.hl7,ignore,HL7_BATCH,5
 *   test-0001-input-covid-19.hl7,ignore.ignore-hl7,test-0001-pima-az-covid-19.csv,ignore,CSV,5
 *
 * LIMITATIONS: Only supports CSV and HL7_BATCH receivers.  Tests are run sequentially.
 */
class DataCompareTest : CoolTest() {
    override val name = "compare"
    override val description = "Send data to API based on config file, check lineage and compare data against expected"
    override val status = TestStatus.DRAFT

    private val testDataDir = "/clitests/compare-test-files"
    private val testConfigFile = "compare-test-config.csv"

    /**
     * The input information for a test.
     */
    data class TestInput(val inputFile: String, val sender: String)

    /**
     * The expected output from ReportStream.
     */
    data class TestOutput(
        val outputFile: String,
        val orgName: String,
        val receiverName: String,
        val expectedCount: Int,
        var receiver: Receiver? = null
    )

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
        SENDER("Sender"),

        /**
         * The organization name for the receiver.
         */
        RECEIVER_ORG_NAME("Organization name"),

        /**
         * The receiver name.
         */
        RECEIVER_NAME("Receiver name"),

        /**
         * The number of expected reports in the output.  This is used to check the results in the lineage.
         */
        EXPECTED_COUNT("Expected count")
    }

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        var passed = true
        val configs = readTestConfig("$testDataDir/$testConfigFile")

        // Go through each input file
        if (configs.isNotEmpty()) {
            configs.forEach { config ->
                // Collect some useful data first
                val input = config.key
                val inputFilePath = "$testDataDir/${config.key.inputFile}"
                val inputFile = this::class.java.getResourceAsStream(inputFilePath)
                val outputList = config.value
                val sender = settings.findSender(input.sender)
                    ?: error("Unable to find sender ${input.sender}")
                val receivers = outputList.map { testOutput -> // TODO do we need receivers
                    // We expect only one receiver back
                    val rec = settings.receivers.filter { receiver ->
                        receiver.organizationName == testOutput.orgName && receiver.name == testOutput.receiverName
                    }
                    if (rec.isNotEmpty()) {
                        if (rec[0].format == Report.Format.HL7_BATCH || rec[0].format == Report.Format.CSV) {
                            testOutput.receiver = rec[0]
                            rec[0]
                        } else {
                            error("This test only supports receivers with CSV and HL7_BATCH formats.")
                        }
                    } else {
                        error("Receiver not found")
                    }
                }

                if (inputFile != null) {
                    // Send the input file to ReportStream
                    val (responseCode, json) =
                        HttpUtilities.postReportBytes(environment, inputFile.readBytes(), sender, options.key)
                    if (responseCode != HttpURLConnection.HTTP_CREATED) {
                        bad("***$name Test FAILED***:  response code $responseCode")
                        passed = false
                    } else {
                        good("Posting of report succeeded with response code $responseCode")
                    }

                    // Check the response from the endpoint
                    TermUi.echo(json)
                    passed = passed and examineResponse(json)

                    // Compare the data
                    val reportId = getReportIdFromResponse(json)
                    if (reportId != null) {
                        // Look at the lineage results
                        waitABit(25, environment)
                        var totalItemCount = 0
                        outputList.forEach { totalItemCount += it.expectedCount }
                        passed = passed and examineLineageResults(reportId, receivers, totalItemCount)

                        // Compare the data
                        outputList.forEach { output ->
                            passed = passed and compareSentReports(
                                reportId,
                                output,
                                options.sftpDir
                            )
                        }
                    }
                } else {
                    error("***$name Test FAILED***: Input file $inputFilePath not found.")
                }
            }
        }
        return passed
    }

    /**
     * Read the configuration file [configPathname] for this test.
     * @return a map of input file to expected results
     */
    private fun readTestConfig(configPathname: String): Map<TestInput, List<TestOutput>> {
        val config = mutableMapOf<TestInput, MutableList<TestOutput>>()
        // Note we can only use input streams since the file may be in a JAR
        val resourceStream = this::class.java.getResourceAsStream(configPathname)
        if (resourceStream != null) {
            csvReader().readAllWithHeader(resourceStream).forEach {
                // Make sure we have all the fields we need.
                if (!it[ConfigColumns.INPUT_FILE.colName].isNullOrBlank() &&
                    !it[ConfigColumns.SENDER.colName].isNullOrBlank() &&
                    !it[ConfigColumns.RECEIVER_ORG_NAME.colName].isNullOrBlank() &&
                    !it[ConfigColumns.RECEIVER_NAME.colName].isNullOrBlank() &&
                    !it[ConfigColumns.EXPECTED_COUNT.colName].isNullOrBlank()
                ) {
                    val input = TestInput(
                        it[ConfigColumns.INPUT_FILE.colName]!!.trim(),
                        it[ConfigColumns.SENDER.colName]!!.trim()
                    )
                    val output = TestOutput(
                        it[ConfigColumns.EXPECTED_FILE.colName]?.trim() ?: "",
                        it[ConfigColumns.RECEIVER_ORG_NAME.colName]!!.trim(),
                        it[ConfigColumns.RECEIVER_NAME.colName]!!.trim(),
                        Integer.parseInt(it[ConfigColumns.EXPECTED_COUNT.colName]!!)
                    )

                    // Add to an existing input file config if it exists, otherwise create a new one
                    if (config.containsKey(input)) {
                        config[input]!!.add(output)
                    } else {
                        config[input] = mutableListOf(output)
                    }
                } else {
                    error("***$name Test FAILED***: One or more config columns in $configPathname are empty.")
                }
            }
        }
        return config
    }

    /**
     * Compare the expected reports for the [reportId] to the actual [output] by getting the outputs from the
     * database and comparing them to the expected results saved to the [sftpDir] as specified in the config
     * file for the test.
     * @return true if all actuals match the expected values, false otherwise
     */
    private fun compareSentReports(
        reportId: ReportId,
        output: TestOutput,
        sftpDir: String
    ): Boolean {
        var passed = true
        db = WorkflowEngine().db
        db.transact { txn ->
            // Get the output files from the database
            val outputFilename = sftpFilenameQuery(txn, reportId, output.receiver!!.name)
            val outputFile = File(sftpDir, outputFilename)
            val expectedOutputPath = "$testDataDir/${output.outputFile}"
            // Note we can only use input streams since the file may be in a JAR
            val expectedOutputStream = this::class.java.getResourceAsStream(expectedOutputPath)
            val schema = metadata.findSchema(output.receiver!!.schemaName)
            if (outputFilename != null && outputFile.canRead() && expectedOutputStream != null && schema != null) {
                TermUi.echo("----------------------------------------------------------")
                TermUi.echo("Comparing expected data from $expectedOutputPath")
                TermUi.echo("with actual data from $sftpDir/$outputFilename")
                TermUi.echo("using schema ${schema.name}...")
                val result = CompareData().compare(
                    expectedOutputStream, outputFile.inputStream(),
                    output.receiver!!.format, schema
                )
                if (result.passed) {
                    good("Test passed: Data comparison")
                } else {
                    bad("***$name Test FAILED***: Data comparison FAILED")
                }
                if (result.errors.size > 0) bad(result.errors.joinToString("\n", "ERROR: "))
                if (result.warnings.size > 0) TermUi.echo(result.warnings.joinToString("\n", "WARNING: "))
                TermUi.echo("")
                passed = passed and result.passed
            }
        }
        return passed
    }
}

/**
 * Compares two data files or messages.
 */
class CompareData {
    /**
     * The result of the comparison including warnings and errors.
     */
    data class Result(
        var passed: Boolean = true,
        val errors: ArrayList<String> = ArrayList(),
        val warnings: ArrayList<String> = ArrayList()
    ) {
        /**
         * Merge results by adding [anotherResult] errors and warnings and setting the passed flag.
         */
        fun merge(anotherResult: Result) {
            passed = passed and anotherResult.passed
            errors.addAll(anotherResult.errors)
            warnings.addAll(anotherResult.warnings)
        }
    }

    /**
     * Compares two files, an [actual] and [expected], of the same [schema] and [format].
     * @return the result for the comparison, with result.passed true if the comparison was successful
     */
    fun compare(
        expected: File,
        actual: File,
        format: Report.Format,
        schema: Schema
    ): Result {
        val result = Result()
        fun checkFile(file: File) {
            if (!file.canRead()) {
                result.errors.add("Unable to read ${file.absolutePath}")
                result.passed = false
            }
        }

        checkFile(expected)
        checkFile(actual)
        if (result.passed) {
            compare(
                expected.inputStream(), actual.inputStream(),
                format, schema, result
            )
        }
        return result
    }

    /**
     * Compares two input streams, an [actual] and [expected], of the same [schema] and [format].
     * @return the result for the comparison, with result.passed true if the comparison was successful
     */
    fun compare(
        expected: InputStream,
        actual: InputStream,
        format: Report.Format,
        schema: Schema,
        result: Result = Result()
    ): Result {
        if (format == Report.Format.HL7 || format == Report.Format.HL7_BATCH) {
            result.merge(CompareHl7Data().compare(expected, actual))
        } else {
            result.merge(CompareCsvData().compare(expected, actual, schema))
        }
        return result
    }
}

/**
 * Compares two HL7 files.
 */
class CompareHl7Data(val result: CompareData.Result = CompareData.Result()) {
    /**
     * The list of fields that contain dynamic values that cannot be compared.  Source:
     * Hl7Serializer.setLiterals()
     */
    internal val dynamicHl7Values = arrayOf("MSH-7", "SFT-2", "SFT-4", "SFT-6")

    /**
     * Compare the data in the [actual] report to the data in the [expected] report.  This
     * comparison uses steps through all the segments in the HL7 messages and compares all the values in the
     * existing fields.
     * @return the result for the comparison, with result.passed true if the comparison was successful
     * Errors are generated when:
     *  1. The number of reports is different
     *  2. A segment in the expected values does not exist in the actual values
     *  3. A component expected value does not match the actual value
     *
     * Warnings are generated when:
     *  1. A component actual value exists, but no expected value.
     */
    fun compare(
        expected: InputStream,
        actual: InputStream,
        result: CompareData.Result = CompareData.Result()
    ): CompareData.Result {
        var passed = true
        val mcf = CanonicalModelClassFactory("2.5.1")
        val hapiContext = DefaultHapiContext()
        hapiContext.modelClassFactory = mcf
        val expectedMsgs = Hl7InputStreamMessageIterator(expected, hapiContext)
        val actualMsgs = Hl7InputStreamMessageIterator(actual, hapiContext)

        var recordNum = 1

        // Loop through the messages.  In the case of a batch message there will be multiple messages
        while (actualMsgs.hasNext()) {
            val actualMsg = actualMsgs.next()
            val expectedMsg = try {
                expectedMsgs.next()
            } catch (e: NoSuchElementException) {
                result.errors.add(
                    "The number of expected messages is less than the actual $recordNum messages."
                )
                passed = false
                break
            }
            val actualTerser = Terser(actualMsg)
            val expectedTerser = Terser(expectedMsg)

            while (true) {
                try {
                    val actualSegmentName = actualTerser.finder.iterate(true, true)
                    val expectedSegmentName = expectedTerser.finder.iterate(true, true)

                    if (actualSegmentName != expectedSegmentName) {
                        result.errors.add(
                            "Actual HL7 segment name does not match expected. " +
                                "Actual: $actualSegmentName, Expected: $expectedSegmentName"
                        )
                        passed = false
                        break
                    }

                    // The HAPI finder iteration does not give a clear indication when it is done with the entire
                    // message vs an error when it is set to not loop, but with loop=true we get an empty segment name.
                    if (actualSegmentName.isNullOrBlank() || expectedSegmentName.isNullOrBlank()) {
                        if (!expectedSegmentName.isNullOrBlank()) {
                            result.errors.add(
                                "There is an extra segment named $expectedSegmentName missing from " +
                                    " the actual message. "
                            )
                            passed = false
                        }
                        break
                    }

                    val actualSegment = actualTerser.getSegment(actualSegmentName)
                    val expectedSegment = expectedTerser.getSegment(expectedSegmentName)
                    if (actualSegment.numFields() != expectedSegment.numFields()) {
                        result.errors.add(
                            "Actual number of fields in HL7 segment does not match expected. " +
                                "Actual: ${actualSegment.numFields()}, Expected: ${expectedSegment.numFields()}"
                        )
                        passed = false
                        break
                    }

                    // Loop through all the fields in the segment.
                    for (fieldIndex in 1..actualSegment.numFields()) {
                        val actualField = actualSegment.getField(fieldIndex)
                        val expectedField = expectedSegment.getField(fieldIndex)
                        passed = passed and compareField(
                            recordNum, "$actualSegmentName-$fieldIndex",
                            actualSegment.names[fieldIndex - 1], actualField, expectedField, result
                        )
                    }
                } catch (e: HL7Exception) {
                    result.errors.add("There was an error parsing the HL7 messages: $e")
                    passed = false
                }
            }
            recordNum++
        }
        result.passed = result.passed and passed
        return result
    }

    /**
     * Compare an [actualFieldContents] to an [expectedFieldContents] HL7 field for a given [recordNum], [fieldSpec],
     * and [fieldName]. All components in a field are compared and dynamic fields are checked
     * they have content.
     * @return true if the field data is equal, false otherwise
     */
    fun compareField(
        recordNum: Int,
        fieldSpec: String,
        fieldName: String,
        actualFieldContents: Array<Type>,
        expectedFieldContents: Array<Type>,
        result: CompareData.Result
    ): Boolean {
        var passed = true
        val maxNumRepetitions = if (actualFieldContents.size > expectedFieldContents.size) actualFieldContents.size
        else expectedFieldContents.size
        if (maxNumRepetitions > 0) {
            // Loop through all the components in a field and compare their values.
            for (repetitionIndex in 0 until maxNumRepetitions) {
                // If this is not a dynamic value then check it against the expected values
                if (!dynamicHl7Values.contains(fieldSpec)) {
                    val expectedFieldValue = if (repetitionIndex < expectedFieldContents.size)
                        expectedFieldContents[repetitionIndex].toString().trim() else ""
                    val actualFieldValue = if (repetitionIndex < actualFieldContents.size)
                        actualFieldContents[repetitionIndex].toString().trim() else ""
                    passed = passed and compareComponent(
                        actualFieldValue, expectedFieldValue, recordNum,
                        "$fieldSpec($repetitionIndex)",
                        fieldName, result
                    )
                }
                // For dynamic values we expect them to be have something
                else if (actualFieldContents[repetitionIndex].isEmpty) {
                    result.errors.add(
                        "No date/time of message for record $recordNum in field $fieldSpec"
                    )
                    passed = false
                }
            }
        }
        result.passed = result.passed and passed
        return passed
    }

    /**
     * Compare the components of a given [actualFieldValue] and [expectedFieldValue] for the given [recordNum],
     * [fieldSpec] and [fieldName]
     * @return true if the field data is equal, false otherwise
     */
    fun compareComponent(
        actualFieldValue: String,
        expectedFieldValue: String,
        recordNum: Int,
        fieldSpec: String,
        fieldName: String,
        result: CompareData.Result
    ): Boolean {
        var passed = true
        // Get the components.  HAPI can return a string with a type (e.g. HD[blah^blah]) or a string
        // with a single value
        val typeRegex = Regex(".*\\[ *(.*) *].*")
        val expectedValueComponents = if (expectedFieldValue.matches(typeRegex)) {
            typeRegex.find(expectedFieldValue)!!.destructured.component1().split("[", "]", "^")
        } else {
            listOf(expectedFieldValue)
        }
        val actualValueComponents = if (actualFieldValue.matches(typeRegex)) {
            typeRegex.find(actualFieldValue)!!.destructured.component1().split("[", "]", "^")
        } else {
            listOf(actualFieldValue)
        }

        // Loop through all the components
        val maxNumComponents = if (actualValueComponents.size > expectedValueComponents.size)
            actualValueComponents.size else expectedValueComponents.size
        for (componentIndex in 0 until maxNumComponents) {
            val expectedComponentValue = if (componentIndex < expectedValueComponents.size)
                expectedValueComponents[componentIndex].trim() else ""
            val actualComponentValue = if (componentIndex < actualValueComponents.size)
                actualValueComponents[componentIndex].trim() else ""

            // If we have more than one component then show the component number is the messages
            val componentSpec = if (maxNumComponents > 1) "$fieldSpec-${componentIndex + 1}" else fieldSpec

            if (expectedComponentValue.isNotBlank() && expectedComponentValue != actualComponentValue) {
                result.errors.add(
                    "Data value does not match in report $recordNum for " +
                        "$componentSpec|$fieldName. Expected: '$expectedComponentValue', " +
                        "Actual: '$actualComponentValue'"
                )
                passed = false
            } else if (expectedComponentValue.isBlank() && actualComponentValue.isNotBlank()) {
                result.warnings.add(
                    "Actual data has value in report $recordNum for " +
                        "$componentSpec|$fieldName but no expected value. Actual: '$actualComponentValue'"
                )
            }
        }
        result.passed = result.passed and passed
        return passed
    }
}

/**
 * Compares two CSV files that use the same schema.
 */
class CompareCsvData {

    /**
     * Compare the data in the [actual] report to the data in the [expected] report that use the same [schema].
     * Errors are generated when:
     *  1. The number of reports is different (number of rows)
     *  2. A column in the expected values does not exist in the actual values
     *  3. A expected value does not match the actual value
     *  4. Cannot find the actual row in the expected records
     *
     * Warnings are generated when:
     *  1. An actual value exists, but no expected value.
     *  2. There are more columns in the actual data than the expected data
     *
     * @return the result for the comparison, with result.passed true if the comparison was successful
     */
    fun compare(
        expected: InputStream,
        actual: InputStream,
        schema: Schema,
        result: CompareData.Result = CompareData.Result()
    ): CompareData.Result {
        val expectedRows = csvReader().readAll(expected)
        val actualRows = csvReader().readAll(actual)
        val schemaMsgIdIndex = schema.findElementColumn("message_id")
        val schemaPatLastNameIndex = schema.findElementColumn("patient_last_name")
        val schemaPatStateIndex = schema.findElementColumn("patient_state")

        // Check to see if the expected CSV file has headers
        var startRowIndex = 0
        if (actualRows.isNotEmpty() && actualRows[0].isNotEmpty()) {
            val elementCol = schema.findElement(actualRows[0][0])
            val csvCol = schema.findElementByCsvName(actualRows[0][0])
            if (elementCol != null || csvCol != null) startRowIndex = 1
        }

        // Sanity check.  The schema need either the message ID, or patient last name and state.
        if (schemaMsgIdIndex == null && (schemaPatLastNameIndex == null || schemaPatStateIndex == null)) {
            error("Schema ${schema.name} needs to have message ID or (patient last name and state) for the test.")
        }

        // Check that we have the same number of records
        if (expectedRows.size == actualRows.size) {
            // Loop through all the actual rows ignoring the header row
            for (i in startRowIndex until actualRows.size) {
                val actualRow = actualRows[i]
                val actualMsgId = if (schemaMsgIdIndex != null) actualRow[schemaMsgIdIndex].trim() else null
                val actualLastName = if (schemaPatLastNameIndex != null)
                    actualRow[schemaPatLastNameIndex].trim() else null
                val actualPatState = if (schemaPatStateIndex != null)
                    actualRow[schemaPatStateIndex].trim() else null

                // Find the expected row that matches the actual record
                val expectedRowRaw = expectedRows.filter {
                    schemaMsgIdIndex != null && it[schemaMsgIdIndex] == actualMsgId ||
                        (
                            schemaPatLastNameIndex != null && schemaPatStateIndex != null &&
                                it[schemaPatLastNameIndex] == actualLastName &&
                                it[schemaPatStateIndex] == actualPatState
                            )
                }
                if (expectedRowRaw.size == 1) {
                    if (!compareCsvRow(actualRow, expectedRowRaw[0], schema, i, result)) {
                        result.errors.add("Comparison for row #$i FAILED")
                    }
                } else {
                    result.errors.add(
                        "Could not find row in expected data for message $i. " +
                            "Was looking for ID='$actualMsgId', or patient last name='$actualLastName' " +
                            "and state='$actualPatState'"
                    )
                    result.passed = false
                }
            }
        } else {
            result.errors.add(
                "Number of records does not match.  Expected ${expectedRows.size - 1} " +
                    "but got ${actualRows.size - 1}"
            )
            result.passed = false
        }

        return result
    }

    /**
     * Compare the data in the [actualRow] report to the data in the [expectedRow] report that use the same [schema].
     * Errors are generated when:
     *  1. The number of columns in the actual data is less than in the expected data
     *  2. A column in the expected values does not exist in the actual values
     *  3. A expected value does not match the actual value
     *
     * Warnings are generated when:
     *  1. The number of columns in the actual data is more than in the expected data
     *  2. An actual data value exists, but there is no matching expected data
     *
     * @return true if the field data is equal, false otherwise
     */
    fun compareCsvRow(
        actualRow: List<String>,
        expectedRow: List<String>,
        schema: Schema,
        actualRowNum: Int,
        result: CompareData.Result
    ): Boolean {
        var passed = true
        if (actualRow.isEmpty()) {
            result.errors.add(
                "The actual report had no rows"
            )
            passed = false
        } else if (actualRow.size < expectedRow.size) {
            result.errors.add(
                "There are too few columns in the actual report.  Expected ${expectedRow.size} or more, but got" +
                    "${actualRow.size}"
            )
            passed = false
        } else {
            if (actualRow.size > expectedRow.size) {
                result.warnings.add(
                    "Actual report has more columns than expected.  Actual has ${actualRow.size} and expected " +
                        "${expectedRow.size}"
                )
            }

            // Loop through all the expected columns ignoring the header row
            for (j in expectedRow.indices) {
                val colName = schema.elements[j].name

                if (expectedRow[j].isNotBlank()) {
                    val actualValue = actualRow[j].trim()
                    val expectedValue = expectedRow[j].trim()
                    // For date/time values, the string has timezone offsets that can differ per environment, so
                    // compare the numeric value instead of just the string
                    val a = schema.elements[j].type
                    if (schema.elements[j].type != null &&
                        schema.elements[j].type == Element.Type.DATETIME && actualRow[j].isNotBlank()
                    ) {
                        try {
                            val expectedTime =
                                OffsetDateTime.parse(expectedRow[j].trim(), Element.datetimeFormatter).toEpochSecond()
                            val actualTime =
                                OffsetDateTime.parse(actualRow[j].trim(), Element.datetimeFormatter).toEpochSecond()
                            if (expectedTime != actualTime) {
                                result.errors.add(
                                    "Date time value does not match in report $actualRowNum " +
                                        "column #${j + 1}, '$colName'. Expected: '${expectedRow[j].trim()}', " +
                                        "Actual: '${actualRow[j].trim()}, EpochSec: $expectedTime/$actualTime'"
                                )
                                passed = false
                            }
                        } catch (e: DateTimeParseException) {
                            result.errors.add(
                                "Error while parsing date/time from values in report $actualRowNum column #${j + 1}, " +
                                    "'$colName'. Expected: '${expectedRow[j].trim()}', " +
                                    "Actual: '${actualRow[j].trim()}'"
                            )
                            passed = false
                        }
                    } else if (actualRow[j].trim() != expectedRow[j].trim()) {
                        result.errors.add(
                            "Data value does not match in report $actualRowNum column #${j + 1}, " +
                                "'$colName'. Expected: '${expectedRow[j].trim()}', " +
                                "Actual: '${actualRow[j].trim()}'"
                        )
                        passed = false
                    }
                } else if (actualRow[j].isNotBlank()) {
                    result.warnings.add(
                        "Actual data has value in report $actualRowNum for column " +
                            "'$colName' - column #${j + 1}, but no expected value.  " +
                            "Actual: '${actualRow[j].trim()}'"
                    )
                }
            }
        }
        result.passed = result.passed and passed
        return passed
    }
}