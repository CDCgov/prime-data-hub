package gov.cdc.prime.router.tokens

import gov.cdc.prime.router.secrets.SecretHelper
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import javax.crypto.SecretKey
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.io.Encoders

interface ReportStreamSecretFinder {
    fun getReportStreamTokenSigningSecret(): SecretKey
}

class FindReportStreamSecretInVault : ReportStreamSecretFinder {
    override fun getReportStreamTokenSigningSecret(): SecretKey {
        val secretServiceAgent = SecretHelper.getSecretService()
        val secret = secretServiceAgent.fetchSecret(TOKEN_SIGNING_SECRET_NAME)
            ?: error("Unable to find secret $TOKEN_SIGNING_SECRET_NAME.  Did you forget to create it?" +
                " If localhost, generate key using ReportStreamSecretFinder:main, then place in" +
                " docker-compose like this:       - TokenSigningSecret=<secret>")
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
    }

    companion object {
        const val TOKEN_SIGNING_SECRET_NAME = "TokenSigningSecret"
        private val TOKEN_SIGNING_KEY_ALGORITHM = SignatureAlgorithm.HS384
        // convenience method that knows how to generate the right kind of secret.
        fun generateSecret(): String {
            return Encoders.BASE64.encode(Keys.secretKeyFor(TOKEN_SIGNING_KEY_ALGORITHM).encoded)
        }
    }
}

/**
 * Convenience function to generate a key to be used as a ReportStream secret
 */
fun main(args: Array<String>) {
    println("Put this env var in your docker-compose file:")
    println(FindReportStreamSecretInVault.TOKEN_SIGNING_SECRET_NAME + "=" +
        FindReportStreamSecretInVault.generateSecret())
}

/**
 * Return a ReportStream secret, used by ReportStream to sign a short-lived token
 * This stores a secret in static memory.  For testing only.
 */
class GetStaticSecret: ReportStreamSecretFinder {
    var tokenSigningSecret = "UVD4QOJ3H295Zi9Ayl3ySuoXNKiE8WYuOsaXOZfug3dwTUVBC1ZIKRPpG5LEyZDZ"

    override fun getReportStreamTokenSigningSecret(): SecretKey {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(tokenSigningSecret))
    }
}



