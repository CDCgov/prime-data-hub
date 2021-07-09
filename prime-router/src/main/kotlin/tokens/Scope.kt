package gov.cdc.prime.router.tokens

import gov.cdc.prime.router.Sender
import org.apache.logging.log4j.kotlin.Logging
import java.lang.IllegalArgumentException

/**
 * A set of helper functions related to validating scopes.
 *
 * In ReportStream, a scope is of the form senderFullname.detailedScope,
 * that is, orgName.senderName.detailedScope
 * Example:  strac.default.report
 */
class Scope {
    companion object: Logging {
        enum class DetailedScope {
            report,  // ability to submit a report
        }

        fun isWellFormedScope(scope: String): Boolean {
            val splits = scope.split(".")
            if (splits.size != 3) {
                logger.warn("Scope should be org.sender.endpoint.  Instead got: $scope ")
                return false
            }
            return true
        }

        fun isValidScope(scope: String, expectedSender: Sender): Boolean {
            if (!isWellFormedScope(scope)) return false
            val splits = scope.split(".")
            if (splits[0] != expectedSender.organizationName) {
                logger.warn("Expected organization ${expectedSender.organizationName}. Instead got: ${splits[0]}")
                return false
            }
            if (splits[1] != expectedSender.name) {
                logger.warn("Expected sender ${expectedSender.name}. Instead got: ${splits[1]}")
                return false
            }
            try {
                DetailedScope.valueOf(splits[2])
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid DetailedScope ${splits[2]}")
                return false
            }
            return true
        }

        fun generateValidScope(sender: Sender, detailedScope: String): String {
            return "${sender.fullName}.$detailedScope"
        }

        fun scopeListContainsScope(scopeList: String, desiredScope: String): Boolean {
            if (desiredScope.isBlank() || desiredScope.isEmpty()) return false
            // A scope is a set of strings separated by single spaces
            val scopesTrial: List<String> = scopeList.split(" ")
            return scopesTrial.contains(desiredScope)
        }


    }
}