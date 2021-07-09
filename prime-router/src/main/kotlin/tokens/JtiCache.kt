package gov.cdc.prime.router.tokens

import org.apache.logging.log4j.kotlin.Logging
import java.time.OffsetDateTime

/**
 * The JTI is a standard claim in a JWT.  It is nonce string value that uniquely identifies a JWT.
 * The JtiCache is used to check whether this JTI (and hence, this JWT) has been encountered before.
 * If it has, the auth request is rejected, to prevent replay attacks.
 *
 * Note:  DPC project implements very similar -
 * See https://github.com/CMSgov/dpc-app/blob/master/dpc-api/src/main/java/gov/cms/dpc/api/auth/jwt/IJTICache.java
 */

abstract class JtiCache: Logging {
    abstract fun cleanupCache()

    abstract fun insertIntoCache(jti: String, expiresAt: OffsetDateTime)

    abstract fun isPresentInCache(jti: String): Boolean

    /**
     * Rather than having a timer based cache cleanup, this lazily only cleans up stale cache entries when its called.
     */
    fun isJTIOk(jti: String, expiresAt: OffsetDateTime): Boolean {
        cleanupCache()
        return if (isPresentInCache(jti)) {
            logger.warn("JTI $jti is being replayed")
            false
        } else {
            // Never expire in less than 5 minutes.
            val minimumExpirationTime = OffsetDateTime.now().plusMinutes(5)
            if (expiresAt.isBefore(minimumExpirationTime)) {
                insertIntoCache(jti, minimumExpirationTime)
            } else {
                insertIntoCache(jti, expiresAt)
            }
            true
        }
    }
}