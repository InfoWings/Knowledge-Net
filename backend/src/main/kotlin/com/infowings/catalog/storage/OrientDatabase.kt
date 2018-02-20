package com.infowings.catalog.storage

import com.orientechnologies.orient.core.db.ODatabasePool
import com.orientechnologies.orient.core.db.ODatabaseType
import com.orientechnologies.orient.core.db.OrientDB
import com.orientechnologies.orient.core.db.OrientDBConfig
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResult
import com.orientechnologies.orient.core.sql.executor.OResultSet
import com.orientechnologies.orient.core.tx.OTransaction
import javax.annotation.PreDestroy


class OrientDatabase(url: String, database: String, user: String, password: String) {

    private var orientDB = OrientDB(url, user, password, OrientDBConfig.defaultConfig())
    private var dbPool = ODatabasePool(orientDB, database, "admin", "admin")

    /**
     * DO NOT use directly, use [transaction] and [session] instead
     */
    fun acquire(): ODatabaseDocument = dbPool.acquire()

    init {
        // злой хак для тестов
        if (url == "memory")
            orientDB.create(database, ODatabaseType.MEMORY)

        // создаем необходимые классы
        OrientDatabaseInitializer(this)
            .initAspects()
            .initUsers()
            .initMeasures()
    }

    @PreDestroy
    fun cleanUp() {
        dbPool.close()
        orientDB.close()
    }

    /**
     * run specified query and process result in provided lambda
     *
     * usage example:
     *
     * database.query(selectFromAspect) { rs, session ->
     * rs.mapNotNull { it.toVertexOrNUll()?.toAspect(session) }.toList()}
     */
    fun <T> query(query: String, vararg args: Any, block: (Sequence<OResult>) -> T): T {
        return session(database = this) { session ->
            return@session session.query(query, *args)
                .use { rs: OResultSet -> block(rs.asSequence()) }
        }
    }
}

val sessionStore: ThreadLocal<ODatabaseDocument> = ThreadLocal()

/**
 * DO NOT use directly, use [transaction] and [session] instead
 */
inline fun <U> transactionInner(
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

/**
 * Use this function when you do not need database access to be transactional (reads or single document access are atomic)
 *
 * sessions can be nested
 */
inline fun <U> session(database: OrientDatabase, block: (db: ODatabaseDocument) -> U): U {
    val session = sessionStore.get()

    if (session != null)
        return block(session)

    val newSession = database.acquire()
    try {
        sessionStore.set(newSession)
        return block(newSession)
    } finally {
        sessionStore.remove()
        newSession.close()
    }
}

/**
 * use this to get transactional access
 * transactions can be nested, sessions nested into transaction will become transaction
 */
inline fun <U> transaction(
    database: OrientDatabase,
    retryOnFailure: Int = 0,
    txtype: OTransaction.TXTYPE = OTransaction.TXTYPE.OPTIMISTIC,
    block: (db: ODatabaseDocument) -> U
): U {
    val session = sessionStore.get()
    if (session != null)
        return transactionInner(session, retryOnFailure, txtype, block)

    val newSession = database.acquire()
    try {
        sessionStore.set(newSession)
        return transactionInner(newSession, retryOnFailure, txtype, block)
    } finally {
        sessionStore.remove()
        newSession.close()
    }
}

operator fun <T> OVertex.get(name: String): T = getProperty(name)
operator fun OVertex.set(name: String, value: Any?) = setProperty(name, value)

fun OResult.toVertex(): OVertex = vertex.orElse(null) ?: throw OrientException("Not a vertex")
fun OResult.toVertexOrNUll(): OVertex? = vertex.orElse(null)

class OrientException(reason: String) : Throwable(reason)
