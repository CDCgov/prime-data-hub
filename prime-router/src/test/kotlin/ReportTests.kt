package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail

class ReportTests {
    private val metadata: Metadata = Metadata("./metadata")

    val rcvr = Receiver("name", "org", "topic", "schema", Report.Format.CSV)

    @Test
    fun `test merge`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        val report2 = Report(one, listOf(listOf("5", "6"), listOf("7", "8")), source = TestSource)
        val mergedReport = Report.merge(listOf(report1, report2))
        assertThat(4).isEqualTo(mergedReport.itemCount)
        assertThat(2).isEqualTo(report1.itemCount)
        assertThat("8").isEqualTo(mergedReport.getString(3, "b"))
        assertThat(2).isEqualTo(mergedReport.sources.size)
        assertThat(report1.id).isEqualTo((mergedReport.sources[0] as ReportSource).id)
    }

    @Test
    fun `test filter`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val jurisdictionalFilter = metadata.findJurisdictionalFilter("matches") ?: fail("cannot find filter")
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        assertThat(2).isEqualTo(report1.itemCount)
        val filteredReport = report1.filter(listOf(Pair(jurisdictionalFilter, listOf("a", "1"))), rcvr, false)
        assertThat(one).isEqualTo(filteredReport.schema)
        assertThat(1).isEqualTo(filteredReport.itemCount)
        assertThat("2").isEqualTo(filteredReport.getString(0, "b"))
        assertThat(1).isEqualTo(filteredReport.sources.size)
    }

    @Test
    fun `test multiarg matches filter`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val jurisdictionalFilter = metadata.findJurisdictionalFilter("matches") ?: fail("cannot find filter")
        // each sublist is a row.
        val report1 = Report(one, listOf(listOf("row1_a", "row1_b"), listOf("row2_a", "row2_b")), source = TestSource)
        assertThat(2).isEqualTo(report1.itemCount)
        val filteredReportA = report1.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "row1.*", "row2_a"))), rcvr, false
        )
        assertThat(2).isEqualTo(filteredReportA.itemCount)
        assertThat("row1_b").isEqualTo(filteredReportA.getString(0, "b"))
        assertThat("row2_b").isEqualTo(filteredReportA.getString(1, "b"))

        val filteredReportB = report1.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "row.*"))), rcvr, false
        )
        assertThat(2).isEqualTo(filteredReportA.itemCount)
        assertThat("row1_b").isEqualTo(filteredReportB.getString(0, "b"))
        assertThat("row2_b").isEqualTo(filteredReportB.getString(1, "b"))

        val filteredReportC = report1.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "row1_a", "foo", "bar", "baz"))), rcvr, false
        )
        assertThat(1).isEqualTo(filteredReportC.itemCount)
        assertThat("row1_b").isEqualTo(filteredReportC.getString(0, "b"))

        val filteredReportD = report1.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "argle", "bargle"))), rcvr, false
        )
        assertThat(0).isEqualTo(filteredReportD.itemCount)
    }

    @Test
    fun `test isEmpty`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val emptyReport = Report(one, emptyList(), source = TestSource)
        assertThat(true).isEqualTo(emptyReport.isEmpty())
        val report1 = Report(one, listOf(listOf("1", "2")), source = TestSource)
        assertThat(false).isEqualTo(report1.isEmpty())
    }

    @Test
    fun `test create with list`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val report1 = Report(one, listOf(listOf("1", "2")), TestSource)
        assertThat(one).isEqualTo(report1.schema)
        assertThat(1).isEqualTo(report1.itemCount)
        assertThat(TestSource).isEqualTo(report1.sources[0] as TestSource)
    }

    @Test
    fun `test applyMapping`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("b")))
        val metadata = Metadata()
        metadata.loadSchemas(one, two)

        val oneReport = Report(schema = one, values = listOf(listOf("a1", "b1"), listOf("a2", "b2")), TestSource)
        assertThat(2).isEqualTo(oneReport.itemCount)
        val mappingOneToTwo = Translator(metadata, FileSettings())
            .buildMapping(fromSchema = one, toSchema = two, defaultValues = emptyMap())

        val twoTable = oneReport.applyMapping(mappingOneToTwo)
        assertThat(2).isEqualTo(twoTable.itemCount)
        assertThat("b2").isEqualTo(twoTable.getString(1, "b"))
    }

    @Test
    fun `test applyMapping with default`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(Element("a", default = "~"), Element("b"))
        )
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("b")))
        val metadata = Metadata()
        metadata.loadSchemas(one, two)

        val twoReport = Report(schema = two, values = listOf(listOf("b1"), listOf("b2")), source = TestSource)
        assertThat(2).isEqualTo(twoReport.itemCount)
        val mappingTwoToOne = Translator(metadata, FileSettings())
            .buildMapping(fromSchema = two, toSchema = one, defaultValues = emptyMap())

        val oneReport = twoReport.applyMapping(mappingTwoToOne)
        assertThat(2).isEqualTo(oneReport.itemCount)
        assertThat("~").isEqualTo(oneReport.getString(0, "a"))
        assertThat("b2").isEqualTo(oneReport.getString(1, "b"))
    }

    @Test
    fun `test deidentify`() {
        val one = Schema(
            name = "one",
            topic = "test",
            elements = listOf(Element("a", pii = true), Element("b"))
        )

        val oneReport = Report(
            schema = one,
            values = listOf(listOf("a1", "b1"), listOf("a2", "b2")),
            source = TestSource
        )

        val oneDeidentified = oneReport.deidentify()
        assertThat(2).isEqualTo(oneDeidentified.itemCount)
        assertThat("").isEqualTo(oneDeidentified.getString(0, "a"))
        assertThat("b1").isEqualTo(oneDeidentified.getString(0, "b"))
    }

    // Tests for Item lineage
    @Test
    fun `test merge item lineage`() {
        val schema = Schema(name = "one", topic = "test", elements = listOf(Element("a")), trackingElement = "a")
        // each sublist is a row.
        val report1 = Report(schema, listOf(listOf("rep1_row1_a"), listOf("rep1_row2_a")), source = TestSource)
        val report2 = Report(schema, listOf(listOf("rep2_row1_a"), listOf("rep2_row2_a")), source = TestSource)

        val merged = Report.merge(listOf(report1, report2))

        assertThat(4).isEqualTo(merged.itemLineages!!.size)
        val firstLineage = merged.itemLineages!![0]
        assertThat(report1.id).isEqualTo(firstLineage.parentReportId)
        assertThat(0).isEqualTo(firstLineage.parentIndex)
        assertThat(merged.id).isEqualTo(firstLineage.childReportId)
        assertThat(0).isEqualTo(firstLineage.childIndex)
        assertThat("rep1_row1_a").isEqualTo(firstLineage.trackingId)

        val lastLineage = merged.itemLineages!![3]
        assertThat(report2.id).isEqualTo(lastLineage.parentReportId)
        assertThat(1).isEqualTo(lastLineage.parentIndex)
        assertThat(merged.id).isEqualTo(lastLineage.childReportId)
        assertThat(3).isEqualTo(lastLineage.childIndex)
        assertThat("rep2_row2_a").isEqualTo(lastLineage.trackingId)
    }

    @Test
    fun `test split item lineage`() {
        val schema = Schema(name = "one", topic = "test", elements = listOf(Element("a")), trackingElement = "a")
        // each sublist is a row.
        val report1 = Report(schema, listOf(listOf("rep1_row1_a"), listOf("rep1_row2_a")), source = TestSource)

        val reports = report1.split()

        assertThat(2).isEqualTo(reports.size)
        assertThat(1).isEqualTo(reports[0].itemLineages!!.size)
        assertThat(1).isEqualTo(reports[1].itemLineages!!.size)

        val firstLineage = reports[0].itemLineages!![0]
        assertThat(report1.id).isEqualTo(firstLineage.parentReportId)
        assertThat(0).isEqualTo(firstLineage.parentIndex)
        assertThat(reports[0].id).isEqualTo(firstLineage.childReportId)
        assertThat(0).isEqualTo(firstLineage.childIndex)
        assertThat("rep1_row1_a").isEqualTo(firstLineage.trackingId)

        val secondLineage = reports[1].itemLineages!![0]
        assertThat(report1.id).isEqualTo(secondLineage.parentReportId)
        assertThat(1).isEqualTo(secondLineage.parentIndex)
        assertThat(reports[1].id).isEqualTo(secondLineage.childReportId)
        assertThat(0).isEqualTo(secondLineage.childIndex)
        assertThat("rep1_row2_a").isEqualTo(secondLineage.trackingId)
    }

    @Test
    fun `test item lineage after jurisdictional filter`() {
        val schema = Schema(name = "one", topic = "test", elements = listOf(Element("a")), trackingElement = "a")
        val metadata = Metadata(schema = schema)
        val jurisdictionalFilter = metadata.findJurisdictionalFilter("matches") ?: fail("cannot find filter")
        // each sublist is a row.
        val report1 = Report(schema, listOf(listOf("rep1_row1_a"), listOf("rep1_row2_a")), source = TestSource)

        val filteredReport = report1.filter(listOf(Pair(jurisdictionalFilter, listOf("a", "rep1_row2_a"))), rcvr, false)

        val lineage = filteredReport.itemLineages!!
        assertThat(1).isEqualTo(lineage.size)
        assertThat(report1.id).isEqualTo(lineage[0].parentReportId)
        assertThat(1).isEqualTo(lineage[0].parentIndex)
        assertThat(filteredReport.id).isEqualTo(lineage[0].childReportId)
        assertThat(0).isEqualTo(lineage[0].childIndex)
        assertThat("rep1_row2_a").isEqualTo(lineage[0].trackingId)
    }

    @Test
    fun `test merge then split`() {
        val schema = Schema(name = "one", topic = "test", elements = listOf(Element("a")), trackingElement = "a")
        // each sublist is a row.
        val report1 = Report(schema, listOf(listOf("rep1_row1_a"), listOf("rep1_row2_a")), source = TestSource)
        val report2 = Report(schema, listOf(listOf("rep2_row1_a"), listOf("rep2_row2_a")), source = TestSource)

        val merged = Report.merge(listOf(report1, report2))
        val reports = merged.split()

        assertThat(4).isEqualTo(reports.size)
        assertThat(1).isEqualTo(reports[0].itemLineages!!.size)
        assertThat(1).isEqualTo(reports[3].itemLineages!!.size)

        val firstLineage = reports[0].itemLineages!![0]
        assertThat(report1.id).isEqualTo(firstLineage.parentReportId)
        assertThat(0).isEqualTo(firstLineage.parentIndex)
        assertThat(reports[0].id).isEqualTo(firstLineage.childReportId)
        assertThat(0).isEqualTo(firstLineage.childIndex)
        assertThat("rep1_row1_a").isEqualTo(firstLineage.trackingId)

        val fourthLineage = reports[3].itemLineages!![0]
        assertThat(report2.id).isEqualTo(fourthLineage.parentReportId)
        assertThat(1).isEqualTo(fourthLineage.parentIndex)
        assertThat(reports[3].id).isEqualTo(fourthLineage.childReportId)
        assertThat(0).isEqualTo(fourthLineage.childIndex)
        assertThat("rep2_row2_a").isEqualTo(fourthLineage.trackingId)
    }

    @Test
    fun `test lineage insanity`() {
        val schema = Schema(name = "one", topic = "test", elements = listOf(Element("a")), trackingElement = "a")
        // each sublist is a row.
        val report1 = Report(schema, listOf(listOf("bbb"), listOf("aaa"), listOf("aaa")), source = TestSource)
        val metadata = Metadata(schema = schema)
        val jurisdictionalFilter = metadata.findJurisdictionalFilter("matches") ?: fail("cannot find filter")

        // split, merge, split, merge, copy, copy, then filter.
        val reports1 = report1.split()
        val merge1 = Report.merge(reports1)
        val reports2 = merge1.split()
        val merge2 = Report.merge(reports2)
        val copy1 = merge2.copy()
        val copy2 = copy1.copy()
        val filteredReport = copy2.filter(listOf(Pair(jurisdictionalFilter, listOf("a", "aaa"))), rcvr, false)

        val lineage = filteredReport.itemLineages!!
        assertThat(2).isEqualTo(lineage.size)

        assertThat(report1.id).isEqualTo(lineage[0].parentReportId)
        assertThat(1).isEqualTo(lineage[0].parentIndex)
        assertThat(filteredReport.id).isEqualTo(lineage[0].childReportId)
        assertThat(0).isEqualTo(lineage[0].childIndex)
        assertThat("aaa").isEqualTo(lineage[0].trackingId)

        assertThat(report1.id).isEqualTo(lineage[1].parentReportId)
        assertThat(2).isEqualTo(lineage[1].parentIndex)
        assertThat(filteredReport.id).isEqualTo(lineage[1].childReportId)
        assertThat(1).isEqualTo(lineage[1].childIndex)
        assertThat("aaa").isEqualTo(lineage[1].trackingId)
    }

    @Test
    fun `test synthesize data with empty strategy map`() {
        // arrange
        val schema = Schema(
            name = "test",
            topic = "test",
            elements = listOf(
                Element("last_name"), Element("first_name")
            )
        )
        val report = Report(
            schema = schema,
            values = listOf(listOf("smith", "sarah"), listOf("jones", "mary"), listOf("white", "roberta")),
            source = TestSource
        )
        // act
        val synthesizedReport = report.synthesizeData(metadata = metadata)
        // assert
        assertEquals(3, synthesizedReport.itemCount)
        assertEquals("smith", synthesizedReport.getString(0, "last_name"))
        assertEquals("jones", synthesizedReport.getString(1, "last_name"))
        assertEquals("white", synthesizedReport.getString(2, "last_name"))
        assertEquals("sarah", synthesizedReport.getString(0, "first_name"))
        assertEquals("mary", synthesizedReport.getString(1, "first_name"))
        assertEquals("roberta", synthesizedReport.getString(2, "first_name"))
    }

    @Test
    fun `test synthesize data with pass through strategy map`() {
        // arrange
        val schema = Schema(
            name = "test",
            topic = "test",
            elements = listOf(
                Element("last_name"), Element("first_name")
            )
        )
        val report = Report(
            schema = schema,
            values = listOf(listOf("smith", "sarah"), listOf("jones", "mary"), listOf("white", "roberta")),
            source = TestSource
        )
        val strategies = mapOf(
            "last_name" to Report.SynthesizeStrategy.PASSTHROUGH,
            "first_name" to Report.SynthesizeStrategy.PASSTHROUGH
        )
        // act
        val synthesizedReport = report.synthesizeData(strategies, metadata = metadata)
        // assert
        assertThat(3).isEqualTo(synthesizedReport.itemCount)
        assertThat("smith").isEqualTo(synthesizedReport.getString(0, "last_name"))
        assertThat("jones").isEqualTo(synthesizedReport.getString(1, "last_name"))
        assertThat("white").isEqualTo(synthesizedReport.getString(2, "last_name"))
        assertThat("sarah").isEqualTo(synthesizedReport.getString(0, "first_name"))
        assertThat("mary").isEqualTo(synthesizedReport.getString(1, "first_name"))
        assertThat("roberta").isEqualTo(synthesizedReport.getString(2, "first_name"))
    }

    @Test
    fun `test synthesize data with blank strategy`() {
        // arrange
        val schema = Schema(
            name = "test",
            topic = "test",
            elements = listOf(
                Element("last_name"), Element("first_name"), Element("ssn")
            )
        )
        val report = Report(
            schema = schema,
            values = listOf(
                listOf("smith", "sarah", "000000000"),
                listOf("jones", "mary", "000000000"),
                listOf("white", "roberta", "000000000"),
            ),
            source = TestSource
        )
        val strategies = mapOf(
            "last_name" to Report.SynthesizeStrategy.PASSTHROUGH,
            "first_name" to Report.SynthesizeStrategy.PASSTHROUGH,
            "ssn" to Report.SynthesizeStrategy.BLANK,
        )
        // act
        val synthesizedReport = report.synthesizeData(strategies, metadata = metadata)
        // assert
        assertThat(3).isEqualTo(synthesizedReport.itemCount)
        assertThat("smith").isEqualTo(synthesizedReport.getString(0, "last_name"))
        assertThat("jones").isEqualTo(synthesizedReport.getString(1, "last_name"))
        assertThat("white").isEqualTo(synthesizedReport.getString(2, "last_name"))
        assertThat("sarah").isEqualTo(synthesizedReport.getString(0, "first_name"))
        assertThat("mary").isEqualTo(synthesizedReport.getString(1, "first_name"))
        assertThat("roberta").isEqualTo(synthesizedReport.getString(2, "first_name"))
        assertThat("").isEqualTo(synthesizedReport.getString(0, "ssn"))
        assertThat("").isEqualTo(synthesizedReport.getString(1, "ssn"))
        assertThat("").isEqualTo(synthesizedReport.getString(2, "ssn"))
    }

    // ignoring this test for now because shuffling is non-deterministic
    @Test
    @Ignore
    fun `test synthesize data with shuffle strategy`() {
        // arrange
        val schema = Schema(
            name = "test",
            topic = "test",
            elements = listOf(
                Element("last_name"), Element("first_name"),
            )
        )
        val report = Report(
            schema = schema,
            values = listOf(
                listOf("smith", "sarah"),
                listOf("jones", "mary"),
                listOf("white", "roberta"),
                listOf("stock", "julie"),
                listOf("chang", "emily"),
                listOf("rodriguez", "anna"),
            ),
            source = TestSource
        )
        val strategies = mapOf(
            "last_name" to Report.SynthesizeStrategy.SHUFFLE,
            "first_name" to Report.SynthesizeStrategy.SHUFFLE,
        )
        // act
        val synthesizedReport = report.synthesizeData(strategies, metadata = metadata)
        // assert
        // assertNotEquals("smith", synthesizedReport.getString(0, "last_name"))
        assertThat("smith").isNotEqualTo(synthesizedReport.getString(0, "last_name"))
        // assertNotEquals("jones", synthesizedReport.getString(1, "last_name"))
        assertThat("jones").isNotEqualTo(synthesizedReport.getString(1, "last_name"))
        // assertNotEquals("white", synthesizedReport.getString(2, "last_name"))
        assertThat("white").isNotEqualTo(synthesizedReport.getString(2, "last_name"))
        // assertNotEquals("sarah", synthesizedReport.getString(0, "first_name"))
        assertThat("sarah").isNotEqualTo(synthesizedReport.getString(0, "first_name"))
        // assertNotEquals("mary", synthesizedReport.getString(1, "first_name"))
        assertThat("mary").isNotEqualTo(synthesizedReport.getString(1, "first_name"))
        // assertNotEquals("roberta", synthesizedReport.getString(2, "first_name"))
        assertThat("roberta").isNotEqualTo(synthesizedReport.getString(2, "first_name"))
    }
}