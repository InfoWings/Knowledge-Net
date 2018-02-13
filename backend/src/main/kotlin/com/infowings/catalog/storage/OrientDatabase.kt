package com.infowings.catalog.storage

import com.orientechnologies.orient.core.db.*
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.tx.OTransaction
import javax.annotation.PreDestroy


class OrientDatabase(val url: String, val database: String, user: String, password: String) {
    private var orientDB = OrientDB(url, user, password, OrientDBConfig.defaultConfig())
    private var dbPool = ODatabasePool(orientDB, database, "admin", "admin")

    fun acquire(): ODatabaseDocument = dbPool.acquire()

    init {
        // злой хак для тестов
        if (url == "memory")
            orientDB.create(database, ODatabaseType.MEMORY)

        // создаем необходимые классы
        val session = dbPool.acquire()
        session.createClassIfNotExist("Aspect")
        session.close()
    }

    @PreDestroy
    fun cleanUp() {
        dbPool.close()
        orientDB.close()
    }

    fun close() = orientDB.close()
}

inline fun <U> transaction(
    database: OrientDatabase,
    retryOnFailure: Int = 0,
    txtype: OTransaction.TXTYPE = OTransaction.TXTYPE.OPTIMISTIC,
    block: (db: ODatabaseDocument) -> U
): U {
    val db = database.acquire()
    var lastException: Exception? = null

    repeat(times = retryOnFailure + 1) {
        try {
            db.begin(txtype)
            val u = block(db)
            db.commit()

            return u
        } catch (e: Exception) {
            lastException = e
            db.rollback()
        } finally {
            db.close()
        }
    }
    lastException?.let { throw it } ?: throw Exception("Cannot commit transaction, but no exception caught. Fatal failure")
}