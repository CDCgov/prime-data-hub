package gov.cdc.prime.router.tokens

import gov.cdc.prime.router.azure.DatabaseAccess
import java.time.OffsetDateTime

class DatabaseJtiCache(val db: DatabaseAccess): JtiCache() {

    override fun cleanupCache() {
        db.transact { txn ->
            db.deleteExpiredJtis(txn)
        }
    }

    override fun insertIntoCache(jti: String, expiresAt: OffsetDateTime) {
        db.transact { txn -> db.insertJti(jti, expiresAt, txn) }
    }

    override fun isPresentInCache(jti: String): Boolean {
        return db.transactReturning { txn ->
            (db.fetchJti(jti, txn) != null)
        }
    }
}