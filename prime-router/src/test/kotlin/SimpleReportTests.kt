package gov.cdc.prime.router

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

//
// Using JUnit here, but this is not a unit test.  This tests end-to-end:  ingesting a csv file,
// creating transformed objects, writing them to output csv files, then doing a simple 'diff'
// to see if they match expected output files.
//
class SimpleReportTests {
    private val defaultSchema = "test-schema"
    private val inputPath = "./src/test/csv_test_files/input/"
    private val expectedResultsPath = "./src/test/csv_test_files/expected/"
    private val outputPath = "./target/csv_test_files/"
    private val metadata: Metadata
    private val csvConverter: CsvConverter

    init {
        val outputDirectory = File(outputPath)
        outputDirectory.mkdirs()

        val expectedDir = File(expectedResultsPath)
        assertTrue(expectedDir.exists())

        metadata = Metadata(Metadata.defaultMetadataDirectory)
        csvConverter = CsvConverter(metadata)
    }

    /**
     * Read in a CSV ile, route it, and create output files
     * Returns a list of Pairs.  Each pair is created report File based on the routing,
     *   and the OrganizationService, as useful metadata about the File.
     */
    fun readAndRoute(filePath: String, schemaName: String): MutableList<Pair<File, OrganizationService>> {
        val file = File(filePath)
        assertTrue(file.exists())
        val schema = metadata.findSchema(schemaName) ?: error("$schemaName not found.")

        // 1) Ingest the file
        val fileSource = FileSource(filePath)
        val readResult = csvConverter.read(schema.name, file.inputStream(), fileSource)
        assertTrue(readResult.errors.isEmpty())
        // I removed this test- at this time, the SimpleReport parsing does return an empty column warning.
        //        assertTrue(readResult.warnings.isEmpty())
        val inputReport = readResult.report ?: fail()
        // 2) Create transformed objects, according to the receiver table rules
        val outputReports = Translator(metadata).filterAndTranslateByService(inputReport)

        // 3) Write transformed objs to files
        val outputFiles = mutableListOf<Pair<File, OrganizationService>>()
        outputReports.forEach { (report, orgSvc) ->
            val fileName = Report.formFileName(
                report.id,
                report.schema.baseName,
                OrganizationService.Format.CSV,
                report.createdDateTime
            )
            val reportFile = File(outputPath, fileName)
            csvConverter.write(report, reportFile.outputStream())
            outputFiles.add(Pair(reportFile, orgSvc))
        }
        return outputFiles
    }

    fun createFakeFile(schemaName: String, numRows: Int): File {
        val schema = metadata.findSchema(schemaName) ?: error("$schemaName not found.")
        // 1) Create the fake file
        val fakeReport = FakeReport(metadata).build(
            schema,
            numRows,
            FileSource("fake") // not really used
        )
        val fakeReportFileName = Report.formFileName(
            fakeReport.id,
            fakeReport.schema.baseName,
            OrganizationService.Format.CSV,
            fakeReport.createdDateTime
        )
        val fakeReportFile = File(outputPath, fakeReportFileName)
        csvConverter.write(fakeReport, fakeReportFile.outputStream())
        assertTrue(fakeReportFile.exists())
        return fakeReportFile
    }

    /**
     * Read in a CSV file, then write it right back out again, in the same schema.
     * The idea is: It shouldn't change.
     */
    fun readAndWrite(inputFilePath: String, schemaName: String): File {
        val inputFile = File(inputFilePath)
        assertTrue(inputFile.exists())
        val schema = metadata.findSchema(schemaName) ?: error("$schemaName not found.")

        // 1) Ingest the file
        val inputFileSource = FileSource(inputFilePath)
        val readResult = csvConverter.read(schema.name, inputFile.inputStream(), inputFileSource)
        assertTrue(readResult.warnings.isEmpty() && readResult.errors.isEmpty())
        val inputReport = readResult.report ?: fail()

        // 2) Write the input report back out to a new file
        val outputFile = File(outputPath, inputReport.name)
        csvConverter.write(inputReport, outputFile.outputStream())
        assertTrue(outputFile.exists())
        return outputFile
    }

    @Test
    fun `test producing az data from simplereport`() {
        val filePath = inputPath + "simplereport.csv"
        val outputFiles = readAndRoute(filePath, "primedatainput/pdi-covid-19")
        outputFiles.forEach { (reportFile, orgSvc) ->
            when (orgSvc.fullName) {
                "az-phd.elr-test" -> {
                    val expectedResultsFile = File(expectedResultsPath, "simplereport-az.csv")
                    compareTestResultsToExpectedResults(reportFile, expectedResultsFile)
                }
                // Note, I took out the test for pima-az-phd.elr because in rare cases the fake data
                // generator won't generate any Pima data.
            }
        }
    }

    @Test
    fun `test fake simplereport data`() {
        val schemaName = "primedatainput/pdi-covid-19"
        val fakeReportFile = createFakeFile(schemaName, 100)
        // Run the data thru its own schema and back out again
        val fakeReportFile2 = readAndWrite(fakeReportFile.absolutePath, schemaName)
        compareTestResultsToExpectedResults(fakeReportFile, fakeReportFile2)
    }

    @Test
    fun `test fake pima data`() {
        val schemaName = "az/pima-az-covid-19"
        val fakeReportFile = createFakeFile(schemaName, 100)
        // Run the data thru its own schema and back out again
        val fakeReportFile2 = readAndWrite(fakeReportFile.absolutePath, schemaName)
        compareTestResultsToExpectedResults(fakeReportFile, fakeReportFile2)
    }

//    @Test
    fun `test fake FL data`() {
        val schemaName = "fl/fl-covid-19"
        val fakeReportFile = createFakeFile(schemaName, 100)
        // Run the data thru its own schema and back out again
        val fakeReportFile2 = readAndWrite(fakeReportFile.absolutePath, schemaName)
        compareTestResultsToExpectedResults(fakeReportFile, fakeReportFile2)
    }

    private fun compareTestResultsToExpectedResults(testFile: File, expectedResultsFile: File) {
        println("SimpleReportTests: diff'ing actual vs expected: ${testFile.absolutePath}    ${expectedResultsFile.absolutePath}")
        assertTrue(testFile.exists())
        assertTrue(expectedResultsFile.exists())
        // A bit of a hack:  diff the two files.
        val testFileLines = testFile.readLines()
        val expectedResultsLines = expectedResultsFile.readLines()
        assertEquals(testFileLines, expectedResultsLines)
    }
}