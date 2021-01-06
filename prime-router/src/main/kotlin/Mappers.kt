package gov.cdc.prime.router

import java.time.LocalDateTime

/**
 * A *Mapper* is defined as a property of a schema element. It is used to create
 * a value for the element when no value is present. For example, the middle_initial element has
 * this mapper:
 *
 *  `mapper: middleInitial(patient_middle_name)`
 *
 * A mapper object is stateless. It has a name which corresponds to
 * the function name in the property. It has a set of arguments, which
 * corresponding to the arguments of the function. Before applying the
 * mapper, the elementName list is generated from the arguments. All element values
 * are then fetched and provided to the apply function.
 */
interface Mapper {
    /**
     * Name of the mapper
     */
    val name: String

    /**
     *
     * The element names of the values that should be requested. Called before apply.
     *
     * For example, if the schema had a mapper field defined for a `minus` mapper
     *
     * - name: some_element
     *   mapper: minus(x, 1)
     *
     * `valueNames` would be called with an args list of ["x", "1"].
     * The minus mapper would return ["x"]. The minus mapper is treating
     * the second argument as a parameter, not as an element name.
     *
     * @param element that contains the mapper definition
     * @param args from the schema
     */
    fun valueNames(element: Element, args: List<String>): List<String>

    /**
     * Apply the mapper using the values from the current report item. Called after valueNames().
     *
     * For example, if the schema had a mapper field defined for a `minus` mapper
     *
     * - name: some_element
     *   mapper: minus(x, 1)
     *
     * `apply` would be called with an `args` list of ["x", "1"] and a
     * `values` list of [ElementAndValue(Element(x, ...), "9")] where "9" is
     * the value of the x element for the current item. The `apply` method
     * would return "8", thus mapping the `some_element` value to the `x` value minus 1.
     *
     * @param args from the schema
     * @param values that where fetched based on valueNames
     */
    fun apply(element: Element, args: List<String>, values: List<ElementAndValue>): String?
}

data class ElementAndValue(
    val element: Element,
    val value: String
)

class MiddleInitialMapper : Mapper {
    override val name = "middleInitial"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.size != 1) error("Schema Error: Invalid number of arguments")
        return args
    }

    override fun apply(element: Element, args: List<String>, values: List<ElementAndValue>): String? {
        if (values.isEmpty()) {
            return null
        } else {
            if (values.size != 1) error("Found ${values.size} values.  Expecting 1 value. Args: $args, Values: $values")
            return values.first().value.substring(0..0).toUpperCase()
        }
    }
}

/**
 * The args for the use mapper is a list of element names in order of priority.
 * The mapper will use the first with a value
 */
class UseMapper : Mapper {
    override val name = "use"

    override fun valueNames(element: Element, args: List<String>) = args

    override fun apply(element: Element, args: List<String>, values: List<ElementAndValue>): String? {
        return if (values.isEmpty()) {
            null
        } else {
            val (fromElement, fromValue) = values.first()
            when {
                element.type == fromElement.type -> fromValue
                element.type == Element.Type.DATE && fromElement.type == Element.Type.DATETIME -> {
                    LocalDateTime.parse(fromValue, Element.datetimeFormatter).format(Element.dateFormatter)
                }
                element.type == Element.Type.TEXT -> fromValue
                // TODO: Unchecked conversions should probably be removed, but the PIMA schema relies on this, right now.
                else -> fromValue
            }
        }
    }
}

/**
 * The mapper concatenates a list of column values together.
 * Call this like this:
 * concat(organization_name, ordering_facility_name)
 * @todo add a separator arg.
 * @todo generalize this to call any kotlin string function?
 */
class ConcatenateMapper : Mapper {
    override val name = "concat"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.size < 2)
            error(
                "Schema Error: concat mapper expects to concat two or more column names"
            )
        return args
    }

    override fun apply(element: Element, args: List<String>, values: List<ElementAndValue>): String? {
        return if (values.isEmpty()) {
            null
        } else {
            values.joinToString { it.value } // default ", " separator for now.
        }
    }
}

/**
 * The args for the ifPresent mapper are an element name and a value.
 * If the elementName is present, the value is used
 */
class IfPresentMapper : Mapper {
    override val name = "ifPresent"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.size != 2) error("Schema Error: ifPresent expects dependency and value parameters")
        return args.subList(0, 1) // The element name
    }

    override fun apply(element: Element, args: List<String>, values: List<ElementAndValue>): String? {
        return if (values.size == 1) {
            return args[1]
        } else {
            null
        }
    }
}

/**
 * The LookupMapper is used to lookup values from a lookup table
 * The args for the lookup mapper is the name of the element with the index value
 * The table involved is the element.table field
 * The lookupColumn is the element.tableColumn field
 */
class LookupMapper : Mapper {
    override val name = "lookup"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.size !in 1..2)
            error("Schema Error: lookup mapper expected one or two args")
        return args
    }

    override fun apply(element: Element, args: List<String>, values: List<ElementAndValue>): String? {
        return if (values.size != args.size) {
            null
        } else {
            val lookupTable = element.tableRef
                ?: error("Schema Error: could not find table ${element.table}")
            val indexValues = values.map {
                val indexColumn = it.element.tableColumn
                    ?: error("Schema Error: no tableColumn for element ${it.element.name}")
                Pair(indexColumn, it.value)
            }
            val lookupColumn = element.tableColumn
                ?: error("Schema Error: no tableColumn for element ${element.name}")
            lookupTable.lookupValues(indexValues, lookupColumn)
        }
    }
}

/**
 * The obx17 mapper is specific to the LIVD table and the DeviceID field. Do not use in other places.
 *
 * @See <a href=https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification>HHS Submission Guidance</a>Do not use it for other fields and tables.
 */
class Obx17Mapper : Mapper {
    override val name = "obx17"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.isNotEmpty())
            error("Schema Error: obx17 mapper does not expect args")
        return listOf("equipment_model_name")
    }

    override fun apply(element: Element, args: List<String>, values: List<ElementAndValue>): String? {
        return if (values.isEmpty()) {
            null
        } else {
            val (indexElement, indexValue) = values.first()
            val lookupTable = element.tableRef
                ?: error("Schema Error: could not find table '${element.table}'")
            val indexColumn = indexElement.tableColumn
                ?: error("Schema Error: no tableColumn for element '${indexElement.name}'")
            val testKitNameId = lookupTable.lookupValue(indexColumn, indexValue, "Testkit Name ID")
            val testKitNameIdType = lookupTable.lookupValue(indexColumn, indexValue, "Testkit Name ID Type")
            if (testKitNameId != null && testKitNameIdType != null) {
                "${testKitNameId}_$testKitNameIdType"
            } else {
                null
            }
        }
    }
}

/**
 * The obx17Type mapper is specific to the LIVD table and the DeviceID field. Do not use in other places.
 *
 * @See <a href=https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification>HHS Submission Guidance</a>Do not use it for other fields and tables.
 */
class Obx17TypeMapper : Mapper {
    override val name = "obx17Type"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        if (args.isNotEmpty())
            error("Schema Error: obx17Type mapper does not expect args")
        return listOf("equipment_model_name")
    }

    override fun apply(element: Element, args: List<String>, values: List<ElementAndValue>): String? {
        return if (values.isEmpty()) {
            null
        } else {
            val (indexElement, indexValue) = values.first()
            val lookupTable = element.tableRef
                ?: error("Schema Error: could not find table '${element.table}'")
            val indexColumn = indexElement.tableColumn
                ?: error("Schema Error: no tableColumn for element '${indexElement.name}'")
            if (lookupTable.lookupValue(indexColumn, indexValue, "Testkit Name ID") != null) "99ELR" else null
        }
    }
}

/**
 * The obx8 mapper fills in OBX-8. This indicates the normalcy of OBX-5
 *
 * @See <a href=https://confluence.hl7.org/display/OO/Proposed+HHS+ELR+Submission+Guidance+using+HL7+v2+Messages#ProposedHHSELRSubmissionGuidanceusingHL7v2Messages-DeviceIdentification>HHS Submission Guidance</a>Do not use it for other fields and tables.
 */
class Obx8Mapper : Mapper {
    override val name = "obx8"

    override fun valueNames(element: Element, args: List<String>): List<String> {
        return listOf("test_result")
    }

    override fun apply(element: Element, args: List<String>, values: List<ElementAndValue>): String? {
        return if (values.isEmpty() || values.size > 1) {
            null
        } else {
            return when (values[0].value) {
                "260373001" -> "A" // Detected
                "260415000" -> "N" // Not Detected
                "720735008" -> "A" // Presumptive positive
                "42425007" -> "N" // Equivocal
                "260385009" -> "N" // Negative
                "895231008" -> "N" // Not detected in pooled specimen
                "462371000124108" -> "A" // Detected in pooled specimen
                "419984006" -> "N" // Inconclusive
                else -> null
            }
        }
    }
}

object Mappers {
    fun parseMapperField(field: String): Pair<String, List<String>> {
        val match = Regex("([a-zA-Z0-9]+)\\x28([a-z, \\x2E_\\x2DA-Z0-9?&^]*)\\x29").find(field)
            ?: error("Mapper field $field does not parse")
        val args = if (match.groupValues[2].isEmpty())
            emptyList()
        else
            match.groupValues[2].split(',').map { it.trim() }
        val mapperName = match.groupValues[1]
        return Pair(mapperName, args)
    }
}