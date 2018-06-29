package com.infowings.catalog.storage

import com.infowings.catalog.auth.user.UserProperties
import com.infowings.catalog.auth.user.Users
import com.infowings.catalog.loggerFor
import com.orientechnologies.common.io.OIOException
import com.orientechnologies.orient.core.db.*
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.exception.OStorageException
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResult
import com.orientechnologies.orient.core.sql.executor.OResultSet
import com.orientechnologies.orient.core.tx.OTransaction
import com.orientechnologies.orient.core.tx.OTransactionNoTx
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PreDestroy

/**
 * Public OVertex Extensions.
 */
operator fun <T> OVertex.get(name: String): T = getProperty<T>(name)

operator fun OVertex.set(name: String, value: Any?) = setProperty(name, value)

val OElement.id: String
    get() = identity.toString()

fun OResult.toVertex(): OVertex = vertex.orElse(null) ?: throw OrientException("Not a vertex")
fun OResult.toVertexOrNull(): OVertex? = vertex.orElse(null)

var OVertex.name: String
    get() = this[ATTR_NAME]
    set(value) {
        this[ATTR_NAME] = value
    }

var OVertex.description: String?
    get() = this[ATTR_DESC]
    set(value) {
        this[ATTR_DESC] = value
    }

data class Versioned<T>(val entity: T, val version: Int)

/**
 * Main class for work with database
 * */
class OrientDatabase(
    url: String,
    val database: String,
    username: String,
    password: String,
    userProperties: UserProperties
) {
    private var orientDB = OrientDB(url, username, password, OrientDBConfig.defaultConfig())

    private fun createDbPool() = ODatabasePool(orientDB, database, "admin", "admin")

    private val dbPool = AtomicReference<Versioned<ODatabasePool>>(Versioned(createDbPool(), 1))

    /**
     * DO NOT use directly, use [transaction] and [session] instead
     */
    fun getPool(): Versioned<ODatabasePool> = dbPool.get()

    fun acquire(): ODatabaseSession = dbPool.get().entity.acquire()

    /*
      Изначально хотелось обойтись одним экземпляром пула.
      Но ориентовская реализация ведет себя странно и пулом в традиционном смысле слова не является

      На это сделан тикет на ориент https://github.com/orientechnologies/orientdb/issues/8195

      А здесь - наша надстройка для преодоления проблемы

      Что хочется: чтобы был механизм восстановления от сбоев/отказов сети/базы
      Чтобы при сбое попробовали пересоединиться. Если не получилось дали фронту код ответа,
      соответствующий ситуации. И чтобы после воостановления связи сервис работал без перезапуска.

      Без этого механизма, на одном раз и навсегда созданном dbPool одна неудача в соединении выводит систему
      из строя и требует перезапуска

      В методе transactionInner есть некий механизм ловли исключений и повторных попыток, но он не имеет отношентя к
      данной проблеме. Он про исключения внутри транзакции, не требующие пересоединения

      Исходим из следующих предпосылок:

       - одновременно могут быть несколько нитей работаюющих над одним элземпляром dbPool. Они все могут обнаружить
       проблему с соеднением и сделать это не совсем в один и тот же момент
       - закрываться должен ровно тот экзмпляр dbPool, на котором обнаружена проблема
       - если проблему на экземпляре dbPool обнаружили в несколько нитях одновременно, надо чтобы первый удачный
       экземпляр, созданный взамен сломавшегося, использовался всеми
       - не должно оставаться незакрытых экземпляров dbPool. Только тот, который удачно создан
        и является текущим рабочим. Плюс еще могут оставаться незакрытые сессии/транзакции на том пуле, на который
        кто-то пожаловался и его закрыли, но в другой нити транзакция не дошла до операции с базой и не знает, что
        пула больше нет.
       - следует избегать многократных удалений

       Сделано через версионирование пула.
       Каждый клиент получает структуру, в которой лежит экземпляр пула с номером версии.
       При обнаружении проблемы с соединением он запрашивает новый пул, сообщая данные о том, с которым он работад.

       reOpenPool сверяется версии текущего пула и того, на который жалуются.
       Если они совпадают, то это первая жалоба на текущий пул. Он закрывается и вместо него создается новый.
       Если не совпадают, значит значит на этот пуд уже кто-то жаловался и его закрыли.
       Ничего не закрываем и не создаем. Возращаем текущий.
     */

    fun reOpenPool(from: Versioned<ODatabasePool>): Versioned<ODatabasePool> =
        dbPool.accumulateAndGet(from, { current, given ->
            if (current.version == given.version) {
                current.entity.close()
                Versioned(createDbPool(), current.version + 1)
            } else current
        })

    private val logger = loggerFor<OrientDatabase>()

    init {

        // злой хак для тестов
        if (url == "memory") {
            orientDB.create(database, ODatabaseType.MEMORY)
        }

        val users = try {
            val users = userProperties.toUsers()
            if (users.isEmpty()) {
                logger.info("no custom user settings found. Use default ones")
                Users.toList()
            } else {
                users
            }
        } catch (e: IllegalArgumentException) {
            logger.warn("Ill-formed users configuration: $e")
            logger.warn("Going to use default settings instead")

            Users.toList()
        }

        // создаем необходимые классы
        OrientDatabaseInitializer(this)
            .initAspects()
            .initSubject()
            .initSearch()
            .initHistory()
            .initUsers(users)
            .initMeasures()
            .initReferenceBooks()
            .initSubject()
            .initSearchMeasure() // this call should be latest

    }

    @PreDestroy
    fun cleanUp() {
        dbPool.get().entity.close()
        orientDB.close()
    }

    /**
     * run specified query and process result in provided lambda
     *
     * usage example:
     *
     * database.query(selectFromAspect) { rs, session ->
     * rs.mapNotNull { it.toVertexOrNull()?.toAspect(session) }.toList()}
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
     * rs.mapNotNull { it.toVertexOrNull()?.toAspect(session) }.toList()}
     */
    fun <T> query(query: String, args: Map<String, Any?>, block: (Sequence<OResult>) -> T): T {
        return session(database = this) { session ->
            return@session session.query(query, args)
                .use { rs: OResultSet -> block(rs.asSequence()) }
        }
    }

    fun getVertexById(id: String): OVertex? = session(database = this) {
        return@session it.getRecord<OVertex>(ORecordId(id))
    }


    operator fun get(id: String): OVertex = getVertexById(id) ?: throw VertexNotFound(id)

    fun createNewVertex(className: String): OVertex = session(database = this) {
        return@session it.newVertex(className)
    }

    fun delete(v: OVertex): ODatabase<ORecord> = session(database = this) {
        it.delete(v.identity)
    }

    fun <T> command(command: String, vararg args: Any, block: (Sequence<OResult>) -> T): T {
        return session(database = this) { session ->
            return@session session.command(command, *args)
                .use { rs: OResultSet -> block(rs.asSequence()) }
        }
    }

    fun saveAll(vertices: List<OVertex>) = transaction(database = this) {
        vertices.forEach { it.save<OVertex>() }
    }
}

val sessionStore: ThreadLocal<UniqueODatabaseSessionContainer> = ThreadLocal()

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

val orientLogger = loggerFor<OrientDatabase>()

fun handleRetriable(e: Throwable, database: OrientDatabase, pool: Versioned<ODatabasePool>): Throwable {
    orientLogger.info("Thrown $e on acquire. version ${pool.version}")
    database.reOpenPool(pool)
    return e
}

const val POOL_RETRIES = 2

@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
class DatabaseConnectionFailedException(reason: Throwable) : Exception("last thrown: $reason")

inline fun <U> session(database: OrientDatabase, crossinline block: (db: ODatabaseDocument) -> U): U {
    val sessionContainer = sessionStore.get()

    if (sessionContainer != null)
        return block(sessionContainer.session)

    var lastThrown: Throwable? = null

    repeat(times = POOL_RETRIES) {
        val pool = database.getPool()

        try {
            val newSession = pool.entity.acquire()
            try {
                sessionStore.set(newSession.withUUID())
                return block(newSession)
            } finally {
                sessionStore.remove()
                newSession.close()
            }
        } catch (e: OIOException) {
            lastThrown = handleRetriable(e, database, pool)
        } catch (e: OStorageException) {
            lastThrown = handleRetriable(e, database, pool)
        } catch (e: Throwable) {
            orientLogger.info("Thrown $e")
            throw e
        }
    }

    lastThrown?.let {
        throw DatabaseConnectionFailedException(it)
    } ?: throw Exception("failed to complete session without any exception noticed")
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
    val sessionContainer = sessionStore.get()
    if (sessionContainer != null && sessionContainer.session.transaction !is OTransactionNoTx)
        return block(sessionContainer.session)

    if (sessionContainer != null && sessionContainer.session.transaction is OTransactionNoTx)
        return transactionInner(sessionContainer.session, retryOnFailure, txtype, block)

    var lastThrown: Throwable? = null

    repeat(times = POOL_RETRIES) {
        val pool = database.getPool()

        try {
            val newSession = database.acquire()
            try {
                sessionStore.set(newSession.withUUID())
                return transactionInner(newSession, retryOnFailure, txtype, block)
            } finally {
                sessionStore.remove()
                newSession.close()
            }
        } catch (e: OIOException) {
            lastThrown = handleRetriable(e, database, pool)
        } catch (e: OStorageException) {
            lastThrown = handleRetriable(e, database, pool)
        } catch (e: Throwable) {
            orientLogger.info("Thrown $e")
            throw e
        }
    }

    lastThrown?.let {
        throw DatabaseConnectionFailedException(it)
    } ?: throw Exception("failed to complete session without any exception noticed")
}

class OrientException(reason: String) : Exception(reason)
class VertexNotFound(id: String) : Exception("No vertex for id: $id")

private const val selectById = "SELECT FROM ?"

class UniqueODatabaseSessionContainer(val session: ODatabaseSession) {
    val uuid = UUID.randomUUID()!!
}

fun ODatabaseSession.withUUID() = UniqueODatabaseSessionContainer(this)