package com.infowings.common.catalog.storage

import com.infowings.common.catalog.loggerFor
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
        if (url == "memory") {
            orientDB.create(database, ODatabaseType.MEMORY)
        }

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
inline fun <U> transactionUnsafe(
    session: ODatabaseDocument,
    retryOnFailure: Int = 0,
    txtype: OTransaction.TXTYPE = OTransaction.TXTYPE.OPTIMISTIC,
    block: (db: ODatabaseDocument) -> U
): U {
    var lastException: Exception? = null

    repeat(times = retryOnFailure + 1) {
        try {
            session.begin(txtype)
            val u = block(session)
            session.commit()

            return u
        } catch (e: Exception) {
            lastException = e
            session.rollback()
        }
    }
    lastException?.let { throw it }
            ?: throw Exception("Cannot commit transaction, but no exception caught. Fatal failure")
}

inline fun <U> transaction(
    database: OrientDatabase,
    retryOnFailure: Int = 0,
    txtype: OTransaction.TXTYPE = OTransaction.TXTYPE.OPTIMISTIC,
    block: (db: ODatabaseDocument) -> U
): U = database.acquire().use { transactionUnsafe(it, retryOnFailure, txtype, block) }

private val logger = loggerFor<OrientDatabase>()

operator fun <T> OVertex.get(name: String): T = getProperty(name)
operator fun OVertex.set(name: String, value: Any?) = setProperty(name, value)

fun OResult.toVertex(): OVertex = vertex.orElse(null) ?: throw OrientException("Not a vertex")
fun OResult.toVertexOrNUll(): OVertex? = vertex.orElse(null)

class OrientException(reason: String) : Throwable(reason)
