package gov.cdc.prime.router

import com.fasterxml.jackson.annotation.JsonIgnore
import gov.cdc.prime.router.tokens.Jwk
import gov.cdc.prime.router.tokens.JwkSet

/**
 * A `Sender` represents the agent that is sending reports to
 * to the data hub (minus the credentials used by that agent, of course). It
 * contains information about the specific topic and schema that the sender uses.
 */
open class Sender(
    val name: String,
    val organizationName: String,
    val format: Format,
    val topic: String,
    val schemaName: String,
    val keys: List<JwkSet>? = null,  // used to track server-to-server auths for this Sender via public keys sets
) {
    constructor(copy: Sender) : this(
        copy.name,
        copy.organizationName,
        copy.format,
        copy.topic,
        copy.schemaName,
        if (copy.keys != null) ArrayList(copy.keys) else null
    )

    // constructor that copies and adds a key
    constructor(copy: Sender, newScope: String, newJwk: Jwk) : this(
        copy.name,
        copy.organizationName,
        copy.format,
        copy.topic,
        copy.schemaName,
        addJwkSet(copy.keys, newScope, newJwk)
    )

    @get:JsonIgnore
    val fullName: String get() = "$organizationName$fullNameSeparator$name"

    enum class Format(val mimeType: String) {
        CSV("text/csv"),
        HL7("application/hl7-v2"),
    }

    /**
     * Validate the object and return null or an error message
     */
    fun consistencyErrorMessage(metadata: Metadata): String? {
        if (metadata.findSchema(schemaName) == null) return "Invalid schemaName: $schemaName"
        return null
    }

    fun findKeySetByScope(scope: String): JwkSet? {
        if (keys == null) return null
        return keys.find { it.scope == scope }
    }

    companion object {
        const val fullNameSeparator = "."

        /**
         * Copy an old set of authorizations to a new set, and add one to it, if needed.
         * This whole list of lists thing is confusing:
         * The 'orig' obj, and the return val, are list of JwkSets.   And each JwkSet has a list of Jwks.
         */
        fun addJwkSet(orig: List<JwkSet>?, newScope: String, newJwk: Jwk): List<JwkSet> {
            if (orig == null) {
                return listOf(JwkSet(newScope, listOf(newJwk)))  // create brand new
            }
            val newJwkSetList = mutableListOf<JwkSet>()
            var done = false
            orig.forEach {
                if (it.scope == newScope) {
                    if (it.keys.contains(newJwk)) {
                        // The orig already has this key with this scope.  Just use it.
                        newJwkSetList.add(it)
                    } else {
                        // Don't create a whole new JwkSet.   Instead, add this Jwk to existing JwkSet.
                        val newJwkList = it.keys.toMutableList()
                        newJwkList.add(newJwk)
                        newJwkSetList.add(JwkSet(newScope, newJwkList))
                    }
                    done = true
                } else {
                    newJwkSetList.add(it)  // existing different scope, make sure we keep it.
                }
            }
            // If the old/new scopes didn't match, then the new scope was never added.  Add a new JwkSet now.
            if (!done) {
                newJwkSetList.add(JwkSet(newScope, listOf(newJwk)))
            }
            return newJwkSetList
        }

        fun parseFullName(fullName: String): Pair<String, String> {
            val splits = fullName.split(fullNameSeparator)
            return when (splits.size) {
                1 -> Pair(splits[0], "default")
                2 -> Pair(splits[0], splits[1])
                else -> error("Internal Error: Invalid fullName: $fullName")
            }
        }

        fun canonicalizeFullName(fullName: String): String {
            val splits = fullName.split(fullNameSeparator)
            return when (splits.size) {
                1 -> "${fullName}${fullNameSeparator}default"
                2 -> fullName
                else -> error("Internal Error: Invalid fullName: $fullName")
            }
        }
    }
}
