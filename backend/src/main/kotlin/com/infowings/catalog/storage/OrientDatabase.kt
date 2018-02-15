package com.infowings.catalog.storage

import com.infowings.catalog.loggerFor
import com.orientechnologies.orient.core.db.ODatabasePool
import com.orientechnologies.orient.core.db.ODatabaseType
import com.orientechnologies.orient.core.db.OrientDB
import com.orientechnologies.orient.core.db.OrientDBConfig
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResult
import com.orientechnologies.orient.core.tx.OTransaction
import javax.annotation.PreDestroy


class OrientDatabase(url: String, database: String, user: String, password: String) {

    private var orientDB = OrientDB(url, user, password, OrientDBConfig.defaultConfig())
    private var dbPool = ODatabasePool(orientDB, database, "admin", "admin")

    fun acquire(): ODatabaseDocument = dbPool.acquire()

    init {
        // злой хак для тестов
        if (url == "memory")
            orientDB.create(database, ODatabaseType.MEMORY)

        // создаем необходимые классы
        dbPool.acquire().use {
            OrientDatabaseInitializer(it)
                .initAspects()
                .initUsers()
                .initMeasures()
        }
    }

    @PreDestroy
    fun cleanUp() {
        dbPool.close()
        orientDB.close()
    }
}
//todo: сделать вложенные транзакции (например через ThreadLocal), проверить работу repeat -  возможно не стоит закрывать [session]
inline fun <U> transaction(
    database: OrientDatabase,
    retryOnFailure: Int = 0,
    txtype: OTransaction.TXTYPE = OTransaction.TXTYPE.OPTIMISTIC,
    block: (db: ODatabaseDocument) -> U
): U {
    var lastException: Exception? = null

    repeat(times = retryOnFailure + 1) {
        val session = database.acquire()
        try {
            session.begin(txtype)
            val u = block(session)
            session.commit()

            return u
        } catch (e: Exception) {
            lastException = e
            session.rollback()
        } finally {
            session.close()
        }
    }
    lastException?.let { throw it } ?: throw Exception("Cannot commit transaction, but no exception caught. Fatal failure")
}

private val logger = loggerFor<OrientDatabase>()

operator fun <T> OVertex.get(name: String): T = getProperty(name)
operator fun OVertex.set(name: String, value: Any?) = setProperty(name, value)

val OResult.toVertex: OVertex
    get() = vertex.orElse(null) ?: throw OrientException("Not a vertex")

class OrientException(reason: String) : Throwable(reason)
