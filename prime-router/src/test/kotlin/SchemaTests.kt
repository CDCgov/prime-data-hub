package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test

class SchemaTests {
    @Test
    fun `create schema`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        assertThat(one).isNotNull()
    }

    @Test
    fun `compare schemas`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val oneAgain = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        val two = Schema(name = "two", topic = "test", elements = listOf(Element("a"), Element("b")))
        assertThat(one)
            .isEqualTo(oneAgain)
        assertThat(one)
            .isNotEqualTo(two)
    }

    @Test
    fun `find element`() {
        val one = Schema(name = "one", topic = "test", elements = listOf(Element("a"), Element("b")))
        assertThat(one.findElement("a")).isEqualTo(Element("a"))
        assertThat(one.findElement("c")).isNull()
    }
}