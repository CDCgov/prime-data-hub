package gov.cdc.prime.router.tokens

import gov.cdc.prime.router.azure.DatabaseAccess
import java.time.OffsetDateTime

class MemoryJtiCache: JtiCache() {

    // implement the cache as a map from JTI string name to its expiration time
    var cache = mutableMapOf<String,OffsetDateTime>()

    override fun cleanupCache() {
        cache.forEach {
            if (it.value.isBefore(OffsetDateTime.now()))
                cache.remove(it.key)
        }
    }

    override fun insertIntoCache(jti: String, expiresAt: OffsetDateTime) {
        if (cache[jti] != null) error("cannot insert $jti into cache - its already there")
        cache[jti] = expiresAt
    }

    override fun isPresentInCache(jti: String): Boolean {
        return cache[jti] != null
    }
}