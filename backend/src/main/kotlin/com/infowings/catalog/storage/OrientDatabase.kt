package com.infowings.catalog.storage

import com.infowings.catalog.loggerFor
import com.orientechnologies.orient.core.db.*
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResult
import com.orientechnologies.orient.core.sql.executor.OResultSet
import com.orientechnologies.orient.core.tx.OTransaction
import com.orientechnologies.orient.core.tx.OTransactionNoTx
import javax.annotation.PreDestroy

/**
 * Public OVertex Extensions.
 */
operator fun <T> OVertex.get(name: String): T = getProperty<T>(name)

operator fun OVertex.set(name: String, value: Any?) = setProperty(name, value)

val OElement.id: String
    get() = identity.toString()

fun OResult.toVertex(): OVertex = vertex.orElse(null) ?: throw OrientException("Not a vertex")
fun OResult.toVertexOrNUll(): OVertex? = vertex.orElse(null)


/**
 * Main class for work with database
 * */
class OrientDatabase(url: String, database: String, user: String, password: String) {

    private var orientDB = OrientDB(url, user, password, OrientDBConfig.defaultConfig())
    private var dbPool = ODatabasePool(orientDB, database, "admin", "admin")

    /**
     * DO NOT use directly, use [transaction] and [session] instead
     */
    fun acquire(): ODatabaseDocument = dbPool.acquire()

    init {

        // злой хак для тестов
        if (url == "memory") {
            orientDB.create(database, ODatabaseType.MEMORY)
        }

        // создаем необходимые классы
        OrientDatabaseInitializer(this)
            .initAspects()
            .initUsers()
            .initMeasures()
            .initReferenceBooks()
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

    /**
     * run specified query with named params and process result in provided lambda
     *
     * usage example:
     *
     * database.query(selectFromAspect) { rs, session ->
     * rs.mapNotNull { it.toVertexOrNUll()?.toAspect(session) }.toList()}
     */
    fun <T> query(query: String, args: Map<String, Any>, block: (Sequence<OResult>) -> T): T {
        return session(database = this) { session ->
            return@session session.query(query, args)
                .use { rs: OResultSet -> block(rs.asSequence()) }
        }
    }

    fun getVertexById(id: String): OVertex? =
        query(selectById, ORecordId(id)) { it.map { it.toVertexOrNUll() }.firstOrNull() }

    fun createNewVertex(className: String): OVertex = session(database = this) {
        return@session it.newVertex(className)
    }

    fun delete(v: OVertex): ODatabase<ORecord> = session(database = this) {
        return@session it.delete(v.identity)
    }

    fun <T> command(command: String, vararg args: Any, block: (Sequence<OResult>) -> T): T {
        return session(database = this) { session ->
            return@session session.command(command, *args)
                .use { rs: OResultSet -> block(rs.asSequence()) }
        }
    }

    fun saveAll(vertice: List<OVertex>) = session(database = this) {
        for (v in vertice) {
            v.save<OVertex>()
        }
    }
}

val sessionStore: ThreadLocal<ODatabaseDocument> = ThreadLocal()

/**
 * DO NOT use directly, use [transaction] and [session] instead
 */

val transactionLogger = loggerFor<OrientDatabase>()

inline fun <U> transactionInner(
    session: ODatabaseDocument,
    retryOnFailure: Int = 0,
    txtype: OTransaction.TXTYPE = OTransaction.TXTYPE.OPTIMISTIC,
    crossinline block: (db: ODatabaseDocument) -> U
): U {
    var lastException: Exception? = null

    repeat(times = retryOnFailure + 1) {
        try {
            session.begin(txtype)
            val u = block(session)
            session.commit()

            return u
        } catch (e: Exception) {
            transactionLogger.warn("Thrown inside transaction: $e")
            lastException = e
            session.rollback()
        }
    }
    lastException?.let { throw it }
            ?: throw Exception("Cannot commit transaction, but no exception caught. Fatal failure")
}

//todo: test for transaction/session nesting
/**
 * Use this function when you do not need database access to be transactional (reads or single document access are atomic)
 *
 * sessions can be nested
 */
inline fun <U> session(database: OrientDatabase, crossinline block: (db: ODatabaseDocument) -> U): U {
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
    crossinline block: (db: ODatabaseDocument) -> U
): U {
    val session = sessionStore.get()
    if (session != null && session.transaction !is OTransactionNoTx)
        return block(session)

    if (session != null && session.transaction is OTransactionNoTx)
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

class OrientException(reason: String) : Throwable(reason)

private const val selectById = "SELECT FROM ?"
