package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertFails

internal class ElementTests {

    @Test
    fun `create element`() {
        val elem1 = Element(name = "first", type = Element.Type.NUMBER)
        assertThat(elem1).isNotNull()
    }

    @Test
    fun `compare elements`() {
        val elem1 = Element(name = "first", type = Element.Type.NUMBER)
        val elem2 = Element(name = "first", type = Element.Type.NUMBER)
        assertThat(elem1).isEqualTo(elem2)

        val elem3 = Element(name = "first", type = Element.Type.TEXT)
        assertThat(elem1).isNotEqualTo(elem3)
    }

    @Test
    fun `test schema element`() {
        val elem1 = Element(name = "first", type = Element.Type.NUMBER)
        val elem2 = Element(name = "first", type = Element.Type.NUMBER)
        assertThat(elem1).isEqualTo(elem2)
        val elem3 = Element(name = "first", type = Element.Type.TEXT)
        assertThat(elem1).isNotEqualTo(elem3)
    }

    @Test
    fun `test toNormalize altValues`() {
        val one = Element(
            "b",
            type = Element.Type.CODE,
            altValues = listOf(
                // Use french as an alternative display for these code
                ValueSet.Value("Y", "Oui"),
                ValueSet.Value("N", "Non"),
                ValueSet.Value("UNK", "?")
            ),
            csvFields = Element.csvFields("b", format = "\$alt")
        )
        one.toNormalized("Non", "\$alt").run {
            assertThat(this).isEqualTo("N")
        }
        one.toNormalized("Oui", "\$alt").run {
            assertThat(this).isEqualTo("Y")
        }
    }

    @Test
    fun `test toNormalize date`() {
        val one = Element(
            "a",
            type = Element.Type.DATE,
        )
        // Iso formatted should work
        one.toNormalized("1998-03-30").run {
            assertThat(this).isEqualTo("19980330")
        }
        one.toNormalized("1998-30-03", "yyyy-dd-MM").run {
            assertThat(this).isEqualTo("19980330")
        }
    }

    @Test
    fun `test toNormalize dateTime`() {
        val one = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        // Iso formatted should work
        one.toNormalized("1998-03-30T12:00Z").run {
            assertThat(this).isEqualTo("199803301200+0000")
        }
        one.toNormalized("199803300000+0000").run {
            assertThat(this).isEqualTo("199803300000+0000")
        }
        val two = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        val o = ZoneId.of(USTimeZone.CENTRAL.zoneId).rules.getOffset(Instant.now()).toString()
        val offset = if (o == "Z") {
            "+0000"
        } else {
            o.replace(":", "")
        }
        two.toNormalized("19980330", "yyyyMMdd").run {
            assertThat(this).isEqualTo("199803300000$offset")
        }
        val three = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        three.toNormalized("2020-12-09", "yyyy-MM-dd").run {
            assertThat(this).isEqualTo("202012090000$offset")
        }
    }

    @Test
    fun `test toFormatted dateTime`() {
        val one = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        // Iso formatted should work
        one.toFormatted("199803301200").run {
            assertThat(this).isEqualTo("199803301200")
        }
        one.toFormatted("199803300000").run {
            assertThat(this).isEqualTo("199803300000")
        }

        val two = Element(
            "a",
            type = Element.Type.DATETIME,
        )
        // Other formats should work too, including sans the time.
        two.toFormatted("202012090000+0000", "yyyy-MM-dd").run {
            assertThat(this).isEqualTo("2020-12-09")
        }
    }

    @Test
    fun `test toNormalized zip`() {
        val one = Element(
            "a",
            type = Element.Type.POSTAL_CODE,
            csvFields = Element.csvFields("zip")
        )
        one.toNormalized("99999").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toNormalized("99999-9999").run {
            assertThat(this).isEqualTo("99999-9999")
        }
        // format should not affect normalization
        one.toNormalized("999999999", "\$zipFive").run {
            assertThat(this).isEqualTo("999999999")
        }
        one.toNormalized("KY1-6666").run {
            assertThat(this).isEqualTo("KY1-6666")
        } // Cayman zipcode
        one.toNormalized("KX33-77777").run {
            assertThat(this).isEqualTo("KX33-77777")
        } // Letters and numbers, but a made up zipcode
        assertFails {
            one.toNormalized("%%%%XXXX") // Unreasonable
        }
    }

    @Test
    fun `test toNormalized phone`() {
        val one = Element(
            "a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone")
        )
        one.toNormalized("5559938322").run {
            assertThat(this).isEqualTo("5559938322:1:")
        }
        one.toNormalized("1(555)-968-5052").run {
            assertThat(this).isEqualTo("5559685052:1:")
        }
        one.toNormalized("555-968-5052").run {
            assertThat(this).isEqualTo("5559685052:1:")
        }
        one.toNormalized("+1(555)-968-5052").run {
            assertThat(this).isEqualTo("5559685052:1:")
        }
        one.toNormalized("968-5052").run {
            assertThat(this).isEqualTo("0009685052:1:")
        }
        one.toNormalized("+1(555)-968-5052 x5555").run {
            assertThat(this).isEqualTo("5559685052:1:5555")
        }
        // MX phone number
        one.toNormalized("+52-65-8888-8888").run {
            assertThat(this).isEqualTo("6588888888:52:")
        }
    }

    @Test
    fun `test toFormatted phone`() {
        val one = Element(
            "a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone")
        )
        one.toFormatted("5559938322:1:").run {
            assertThat(this).isEqualTo("5559938322")
        }
        one.toFormatted("5559938322:1:", "\$country-\$area-\$exchange-\$subscriber").run {
            assertThat(this).isEqualTo("1-555-993-8322")
        }
        one.toFormatted("5559938322:1:", "(\$area)\$exchange-\$subscriber").run {
            assertThat(this).isEqualTo("(555)993-8322")
        }
    }

    @Test
    fun `test toFormatted zip`() {
        val one = Element(
            "a",
            type = Element.Type.POSTAL_CODE,
            csvFields = Element.csvFields("zip")
        )
        one.toFormatted("99999").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toFormatted("99999-9999").run {
            assertThat(this).isEqualTo("99999-9999")
        }
        one.toFormatted("99999-9999", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("99999-9999")
        }
        one.toFormatted("99999-9999", "\$zipFive").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toFormatted("999999999", "\$zipFive").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toFormatted("999999999", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("99999-9999")
        }
        one.toFormatted("99999", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("99999")
        }
        one.toFormatted("KY1-5555", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("KY1-5555")
        }
        one.toFormatted("XZ5555", "\$zipFivePlusFour").run {
            assertThat(this).isEqualTo("XZ5555")
        }
    }

    @Test
    fun `test inherit from`() {
        val parent = Element(
            "parent",
            type = Element.Type.TEXT,
            csvFields = Element.csvFields("sampleField"),
            documentation = "I am the parent element"
        )
        // instead of using var, just call run and transmogrify the child then and there
        val child = Element("child").run { inheritFrom(parent) }
        assertThat(
            parent.documentation,
            name = "Documentation value didn't carry over from parent"
        ).isEqualTo(child.documentation)
        assertThat(
            parent.type,
            name = "Element type didn't carry over from parent"
        ).isEqualTo(child.type)
    }

    @Test
    fun `test normalize and formatted round-trips`() {
        val postal = Element(
            "a",
            type = Element.Type.POSTAL_CODE,
            csvFields = Element.csvFields("zip")
        )
        assertThat(
            "94040"
        ).isEqualTo(
            postal.toFormatted(postal.toNormalized("94040"))
        )
        assertThat(
            "94040-6000"
        ).isEqualTo(
            postal.toFormatted(postal.toNormalized("94040-6000"))
        )
        assertThat(
            "94040"
        ).isEqualTo(
            postal.toFormatted(postal.toNormalized("94040-3600", Element.zipFiveToken), Element.zipFiveToken)
        )
        assertThat(
            "94040-3600"
        ).isEqualTo(
            postal.toFormatted(
                postal.toNormalized("94040-3600", Element.zipFivePlusFourToken),
                Element.zipFivePlusFourToken
            )
        )

        val telephone = Element(
            "a",
            type = Element.Type.TELEPHONE,
            csvFields = Element.csvFields("phone")
        )
        assertThat(
            "6509999999"
        ).isEqualTo(
            telephone.toFormatted(telephone.toNormalized("6509999999"))
        )
        assertThat(
            "6509999999"
        ).isEqualTo(
            telephone.toFormatted(telephone.toNormalized("+16509999999"))
        )

        val date = Element(
            "a",
            type = Element.Type.DATE,
            csvFields = Element.csvFields("date")
        )
        assertThat(
            "20201220"
        ).isEqualTo(
            date.toFormatted(date.toNormalized("20201220"))
        )
        assertThat(
            "20201220"
        ).isEqualTo(
            date.toFormatted(date.toNormalized("2020-12-20"))
        )

        val datetime = Element(
            "a",
            type = Element.Type.DATETIME,
            csvFields = Element.csvFields("datetime")
        )
        assertThat(
            "202012200000+0000"
        ).isEqualTo(
            datetime.toFormatted(datetime.toNormalized("202012200000+0000"))
        )
        assertThat(
            "202012200000+0000"
        ).isEqualTo(
            datetime.toFormatted(datetime.toNormalized("2020-12-20T00:00Z"))
        )

        val hd = Element(
            "a",
            type = Element.Type.HD,
            csvFields = Element.csvFields("hd")
        )
        assertThat(
            "HDName"
        ).isEqualTo(
            hd.toFormatted(hd.toNormalized("HDName"))
        )
        assertThat("HDName^0.0.0.0.0.1^ISO").isEqualTo(
            postal.toFormatted(hd.toNormalized("HDName^0.0.0.0.0.1^ISO"))
        )

        val ei = Element(
            "a",
            type = Element.Type.EI,
            csvFields = Element.csvFields("ei")
        )
        assertThat("EIName")
            .isEqualTo(
                ei.toFormatted(ei.toNormalized("EIName"))
            )
        assertThat("EIName^EINamespace^0.0.0.0.0.1^ISO")
            .isEqualTo(
                postal.toFormatted(
                    ei.toNormalized("EIName^EINamespace^0.0.0.0.0.1^ISO")
                )
            )
    }

    @Test
    fun `test normalize with multiple csvFields`() {
        val sendingApp = Element(
            "a",
            type = Element.Type.HD,
            csvFields = listOf(
                Element.CsvField("sending_app", format = "\$name"),
                Element.CsvField("sending_oid", format = "\$universalId")
            )
        )
        val normalized = sendingApp.toNormalized(
            listOf(
                Element.SubValue("sending_app", "happy", "\$name"),
                Element.SubValue("sending_oid", "0.0.0.011", "\$universalId")
            )
        )
        assertThat("happy^0.0.0.011^ISO").isEqualTo(normalized)

        val sendingAppName = sendingApp.toFormatted(normalized, Element.hdNameToken)
        assertThat("happy").isEqualTo(sendingAppName)
        val sendingOid = sendingApp.toFormatted(normalized, Element.hdUniversalIdToken)
        assertThat("0.0.0.011").isEqualTo(sendingOid)
    }

    @Test
    fun `test code format field`() {
        val values = ValueSet(
            "test",
            system = ValueSet.SetSystem.LOCAL,
            values = listOf(
                ValueSet.Value(code = "Y", display = "Yes"),
                ValueSet.Value(code = "N", display = "No"),
                ValueSet.Value(code = "U", display = "Unk")
            )
        )
        val one = Element(
            "a",
            valueSet = "test",
            valueSetRef = values,
            type = Element.Type.CODE,
            altValues = listOf(
                ValueSet.Value(code = "Y", display = "Yes"),
                ValueSet.Value(code = "N", display = "No"),
                ValueSet.Value(code = "U", display = "Unknown")
            )
        )
        // because something is a
        assertThat(one.toFormatted("Y", "\$code")).isEqualTo("Y")
        assertThat(one.toNormalized("Y", "\$code")).isEqualTo("Y")
        assertThat(one.checkForError("Y", "\$code")).isNull()
    }

    @Test
    fun `test truncate`() {
        Element(
            name = "uno",
            type = Element.Type.TEXT,
            maxLength = 2,
        ).run {
            assertThat("ab").isEqualTo(this.truncateIfNeeded("abcde"))
        }
        Element(
            name = "dos",
            type = Element.Type.ID_CLIA, // this type is never truncated.
            maxLength = 2,
        ).run {
            assertThat("abcde").isEqualTo(this.truncateIfNeeded("abcde"))
        }
        Element(
            name = "tres",
            type = Element.Type.TEXT,
            maxLength = 20, // max > actual strlen, nothing to truncate
        ).run {
            assertThat("abcde").isEqualTo(this.truncateIfNeeded("abcde"))
        }
        Element( // zilch is an ok valuer = Element(  // maxLength is null, don't truncate.
            name = "cuatro",
            type = Element.Type.TEXT,
        ).run {
            assertThat("abcde").isEqualTo(this.truncateIfNeeded("abcde"))
        }
        Element(
            name = "cinco",
            type = Element.Type.TEXT,
            maxLength = 0, // zilch is an ok value
        ).run {
            assertThat("").isEqualTo(this.truncateIfNeeded("abcde"))
        }
    }
}