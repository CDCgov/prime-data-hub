package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
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
        // assertEquals(4, mergedReport.itemCount)
        assertThat(4).isEqualTo(mergedReport.itemCount)
        // assertEquals(2, report1.itemCount)
        assertThat(2).isEqualTo(report1.itemCount)
        // assertEquals("8", mergedReport.getString(3, "b"))
        assertThat("8").isEqualTo(mergedReport.getString(3, "b"))
        // assertEquals(2, mergedReport.sources.size)
        assertThat(2).isEqualTo(mergedReport.sources.size)
        // assertEquals(report1.id, (mergedReport.sources[0] as ReportSource).id)
        assertThat(report1.id).isEqualTo((mergedReport.sources[0] as ReportSource).id)
    }

    @Test
    fun `test filter`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val jurisdictionalFilter = metadata.findJurisdictionalFilter("matches") ?: fail("cannot find filter")
        val report1 = Report(one, listOf(listOf("1", "2"), listOf("3", "4")), source = TestSource)
        // assertEquals(2, report1.itemCount)
        assertThat(2).isEqualTo(report1.itemCount)
        val filteredReport = report1.filter(listOf(Pair(jurisdictionalFilter, listOf("a", "1"))), rcvr, false)
        // assertEquals(one, filteredReport.schema)
        assertThat(one).isEqualTo(filteredReport.schema)
        // assertEquals(1, filteredReport.itemCount)
        assertThat(1).isEqualTo(filteredReport.itemCount)
        // assertEquals("2", filteredReport.getString(0, "b"))
        assertThat("2").isEqualTo(filteredReport.getString(0, "b"))
        // assertEquals(1, filteredReport.sources.size)
        assertThat(1).isEqualTo(filteredReport.sources.size)
    }

    @Test
    fun `test multiarg matches filter`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val metadata = Metadata(schema = one)
        val jurisdictionalFilter = metadata.findJurisdictionalFilter("matches") ?: fail("cannot find filter")
        // each sublist is a row.
        val report1 = Report(one, listOf(listOf("row1_a", "row1_b"), listOf("row2_a", "row2_b")), source = TestSource)
        // assertEquals(2, report1.itemCount)
        assertThat(2).isEqualTo(report1.itemCount)
        val filteredReportA = report1.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "row1.*", "row2_a"))), rcvr, false
        )
        // assertEquals(2, filteredReportA.itemCount)
        assertThat(2).isEqualTo(filteredReportA.itemCount)
        // assertEquals("row1_b", filteredReportA.getString(0, "b"))
        assertThat("row1_b").isEqualTo(filteredReportA.getString(0, "b"))
        // assertEquals("row2_b", filteredReportA.getString(1, "b"))
        assertThat("row2_b").isEqualTo(filteredReportA.getString(1, "b"))

        val filteredReportB = report1.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "row.*"))), rcvr, false
        )
        // assertEquals(2, filteredReportA.itemCount)
        assertThat(2).isEqualTo(filteredReportA.itemCount)
        // assertEquals("row1_b", filteredReportB.getString(0, "b"))
        assertThat("row1_b").isEqualTo(filteredReportB.getString(0, "b"))
        // assertEquals("row2_b", filteredReportB.getString(1, "b"))
        assertThat("row2_b").isEqualTo(filteredReportB.getString(1, "b"))

        val filteredReportC = report1.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "row1_a", "foo", "bar", "baz"))), rcvr, false
        )
        // assertEquals(1, filteredReportC.itemCount)
        assertThat(1).isEqualTo(filteredReportC.itemCount)
        // assertEquals("row1_b", filteredReportC.getString(0, "b"))
        assertThat("row1_b").isEqualTo(filteredReportC.getString(0, "b"))

        val filteredReportD = report1.filter(
            listOf(Pair(jurisdictionalFilter, listOf("a", "argle", "bargle"))), rcvr, false
        )
        // assertEquals(0, filteredReportD.itemCount)
        assertThat(0).isEqualTo(filteredReportD.itemCount)
    }

    @Test
    fun `test isEmpty`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val emptyReport = Report(one, emptyList(), source = TestSource)
        // assertEquals(true, emptyReport.isEmpty())
        assertThat(true).isEqualTo(emptyReport.isEmpty())
        val report1 = Report(one, listOf(listOf("1", "2")), source = TestSource)
        // assertEquals(false, report1.isEmpty())
        assertThat(false).isEqualTo(report1.isEmpty())
    }

    @Test
    fun `test create with list`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val report1 = Report(one, listOf(listOf("1", "2")), TestSource)
        assertEquals(one, report1.schema)
        assertEquals(1, report1.itemCount)
        assertEquals(TestSource, report1.sources[0] as TestSource)
    }

    @Test
    fun `test applyMapping`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("b")))
        val metadata = Metadata()
        metadata.loadSchemas(one, two)

        val oneReport = Report(schema = one, values = listOf(listOf("a1", "b1"), listOf("a2", "b2")), TestSource)
        assertEquals(2, oneReport.itemCount)
        val mappingOneToTwo = Translator(metadata, FileSettings())
            .buildMapping(fromSchema = one, toSchema = two, defaultValues = emptyMap())

        val twoTable = oneReport.applyMapping(mappingOneToTwo)
        assertEquals(2, twoTable.itemCount)
        assertEquals("b2", twoTable.getString(1, "b"))
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
        assertEquals(2, twoReport.itemCount)
        val mappingTwoToOne = Translator(metadata, FileSettings())
            .buildMapping(fromSchema = two, toSchema = one, defaultValues = emptyMap())

        val oneReport = twoReport.applyMapping(mappingTwoToOne)
        assertEquals(2, oneReport.itemCount)
        assertEquals("~", oneReport.getString(0, colName = "a"))
        assertEquals("b2", oneReport.getString(1, colName = "b"))
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
        assertEquals(2, oneDeidentified.itemCount)
        assertEquals("", oneDeidentified.getString(0, "a"))
        assertEquals("b1", oneDeidentified.getString(0, "b"))
    }

    // Tests for Item lineage
    @Test
    fun `test merge item lineage`() {
        val schema = Schema(name = "one", topic = "test", elements = listOf(Element("a")), trackingElement = "a")
        // each sublist is a row.
        val report1 = Report(schema, listOf(listOf("rep1_row1_a"), listOf("rep1_row2_a")), source = TestSource)
        val report2 = Report(schema, listOf(listOf("rep2_row1_a"), listOf("rep2_row2_a")), source = TestSource)

        val merged = Report.merge(listOf(report1, report2))

        assertEquals(4, merged.itemLineages!!.size)
        val firstLineage = merged.itemLineages!![0]
        assertEquals(report1.id, firstLineage.parentReportId)
        assertEquals(0, firstLineage.parentIndex)
        assertEquals(merged.id, firstLineage.childReportId)
        assertEquals(0, firstLineage.childIndex)
        assertEquals("rep1_row1_a", firstLineage.trackingId)

        val lastLineage = merged.itemLineages!![3]
        assertEquals(report2.id, lastLineage.parentReportId)
        assertEquals(1, lastLineage.parentIndex)
        assertEquals(merged.id, lastLineage.childReportId)
        assertEquals(3, lastLineage.childIndex)
        assertEquals("rep2_row2_a", lastLineage.trackingId)
    }

    @Test
    fun `test split item lineage`() {
        val schema = Schema(name = "one", topic = "test", elements = listOf(Element("a")), trackingElement = "a")
        // each sublist is a row.
        val report1 = Report(schema, listOf(listOf("rep1_row1_a"), listOf("rep1_row2_a")), source = TestSource)

        val reports = report1.split()

        assertEquals(2, reports.size)
        assertEquals(1, reports[0].itemLineages!!.size)
        assertEquals(1, reports[1].itemLineages!!.size)

        val firstLineage = reports[0].itemLineages!![0]
        assertEquals(report1.id, firstLineage.parentReportId)
        assertEquals(0, firstLineage.parentIndex)
        assertEquals(reports[0].id, firstLineage.childReportId)
        assertEquals(0, firstLineage.childIndex)
        assertEquals("rep1_row1_a", firstLineage.trackingId)

        val secondLineage = reports[1].itemLineages!![0]
        assertEquals(report1.id, secondLineage.parentReportId)
        assertEquals(1, secondLineage.parentIndex)
        assertEquals(reports[1].id, secondLineage.childReportId)
        assertEquals(0, secondLineage.childIndex)
        assertEquals("rep1_row2_a", secondLineage.trackingId)
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
        assertEquals(1, lineage.size)
        assertEquals(report1.id, lineage[0].parentReportId)
        assertEquals(1, lineage[0].parentIndex)
        assertEquals(filteredReport.id, lineage[0].childReportId)
        assertEquals(0, lineage[0].childIndex)
        assertEquals("rep1_row2_a", lineage[0].trackingId)
    }

    @Test
    fun `test merge then split`() {
        val schema = Schema(name = "one", topic = "test", elements = listOf(Element("a")), trackingElement = "a")
        // each sublist is a row.
        val report1 = Report(schema, listOf(listOf("rep1_row1_a"), listOf("rep1_row2_a")), source = TestSource)
        val report2 = Report(schema, listOf(listOf("rep2_row1_a"), listOf("rep2_row2_a")), source = TestSource)

        val merged = Report.merge(listOf(report1, report2))
        val reports = merged.split()

        assertEquals(4, reports.size)
        assertEquals(1, reports[0].itemLineages!!.size)
        assertEquals(1, reports[3].itemLineages!!.size)

        val firstLineage = reports[0].itemLineages!![0]
        assertEquals(report1.id, firstLineage.parentReportId)
        assertEquals(0, firstLineage.parentIndex)
        assertEquals(reports[0].id, firstLineage.childReportId)
        assertEquals(0, firstLineage.childIndex)
        assertEquals("rep1_row1_a", firstLineage.trackingId)

        val fourthLineage = reports[3].itemLineages!![0]
        assertEquals(report2.id, fourthLineage.parentReportId)
        assertEquals(1, fourthLineage.parentIndex)
        assertEquals(reports[3].id, fourthLineage.childReportId)
        assertEquals(0, fourthLineage.childIndex)
        assertEquals("rep2_row2_a", fourthLineage.trackingId)
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
        assertEquals(2, lineage.size)

        assertEquals(report1.id, lineage[0].parentReportId)
        assertEquals(1, lineage[0].parentIndex)
        assertEquals(filteredReport.id, lineage[0].childReportId)
        assertEquals(0, lineage[0].childIndex)
        assertEquals("aaa", lineage[0].trackingId)

        assertEquals(report1.id, lineage[1].parentReportId)
        assertEquals(2, lineage[1].parentIndex)
        assertEquals(filteredReport.id, lineage[1].childReportId)
        assertEquals(1, lineage[1].childIndex)
        assertEquals("aaa", lineage[1].trackingId)
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
        assertEquals(3, synthesizedReport.itemCount)
        assertEquals("smith", synthesizedReport.getString(0, "last_name"))
        assertEquals("jones", synthesizedReport.getString(1, "last_name"))
        assertEquals("white", synthesizedReport.getString(2, "last_name"))
        assertEquals("sarah", synthesizedReport.getString(0, "first_name"))
        assertEquals("mary", synthesizedReport.getString(1, "first_name"))
        assertEquals("roberta", synthesizedReport.getString(2, "first_name"))
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
        assertEquals(3, synthesizedReport.itemCount)
        assertEquals("smith", synthesizedReport.getString(0, "last_name"))
        assertEquals("jones", synthesizedReport.getString(1, "last_name"))
        assertEquals("white", synthesizedReport.getString(2, "last_name"))
        assertEquals("sarah", synthesizedReport.getString(0, "first_name"))
        assertEquals("mary", synthesizedReport.getString(1, "first_name"))
        assertEquals("roberta", synthesizedReport.getString(2, "first_name"))
        assertEquals("", synthesizedReport.getString(0, "ssn"))
        assertEquals("", synthesizedReport.getString(1, "ssn"))
        assertEquals("", synthesizedReport.getString(2, "ssn"))
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
        assertNotEquals("smith", synthesizedReport.getString(0, "last_name"))
        assertNotEquals("jones", synthesizedReport.getString(1, "last_name"))
        assertNotEquals("white", synthesizedReport.getString(2, "last_name"))
        assertNotEquals("sarah", synthesizedReport.getString(0, "first_name"))
        assertNotEquals("mary", synthesizedReport.getString(1, "first_name"))
        assertNotEquals("roberta", synthesizedReport.getString(2, "first_name"))
    }
}