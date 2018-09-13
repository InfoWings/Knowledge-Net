package com.infowings.catalog.storage

//import com.infowings.catalog.auth.user.Users
import com.infowings.catalog.loggerFor
import com.orientechnologies.common.io.OIOException
import com.orientechnologies.orient.core.db.*
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.exception.OStorageException
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.index.OIndex
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OProperty
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.executor.OResult
import com.orientechnologies.orient.core.sql.executor.OResultInternal
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

data class DBIndexInfo(
    val name: String,
    val type: String,
    val algorithm: String,
    val indexVersion: Int,
    val valueContainerAlgorithm: String,
    val indexDefinitionClass: String,
    val clusters: Set<String>,
    val indexDefinition: Map<String, String>,
    val metadata: Map<String, String>
) {
    fun nameElements(): List<String> = name.split(".")
}

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

data class Versioned<out T>(val entity: T, val version: Int)

/**
 * Main class for work with database
 *
 * @param testMode needed for workaround against Embedded Orient mode - it fails to shut down in after executing huge number of unit tests
 * to reproduce issue set to false and run more than ~200 tests
 * */
class OrientDatabase(
    url: String,
    val database: String,
    username: String,
    password: String,
    val testMode: Boolean
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

        // создаем необходимые классы
        OrientDatabaseInitializer(this)
            .initAspects()
            .initSubject()
            .initSearch()
            .initHistory()
            .initUsers()
            .initReferenceBooks()
            .initObject()
            .initGuid()
            .initMeasures()
            .initSearchMeasure() // this call should be latest


    }

    @PreDestroy
    fun cleanUp() {
        dbPool.get().entity.close()
        // have no idea why it works, but after a lot of tests orient get stuck in some monitor in shutdown hook. removing close() fixes this.
        // [testMode] is a guard property to properly close Orient in production mode
        if (!testMode) orientDB.close()
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

    fun <T> getVertexById(id: String, convert: (OVertex) -> T): T? = session(database = this) {
        return@session getVertexById(id)?.let { convert(it) }
    }

    operator fun get(id: String): OVertex = getVertexById(id) ?: throw VertexNotFound(id)

    fun createNewVertex(className: String): OVertex = session(database = this) {
        return@session it.newVertex(className)
    }

    fun delete(v: ORecord): ODatabase<ORecord> = session(database = this) {
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


    private fun luceneIndexName(classType: String, attrName: String) = "$classType.lucene.$attrName"

    fun createLuceneIndex(classType: String, attrName: String) = createLuceneIndex(classType, luceneIndexName(classType, attrName), attrName)

    private fun createLuceneIndex(classType: String, indexName: String, attrName: String) = session(this) { session ->
        val oClass = session.getClass(classType)
        if (oClass.getClassIndex(indexName) == null) {
            val metadata = ODocument()
            metadata.setProperty("allowLeadingWildcard", true)
            CreateIndexWrapper.createIndexWrapper(oClass, indexName, "FULLTEXT", null, metadata, "LUCENE", arrayOf(attrName))
        }
    }

    fun removeIndex(classType: String, indexName: String) = session(this) { session ->
        val oClass = session.getClass(classType)
        val index = oClass.getClassIndex(indexName)
        if (oClass.getClassIndex(indexName) != null) {
            session.command("drop index $indexName")
        }
    }

    fun resetLuceneIndex(classType: String, indexName: String) {
        removeIndex(classType, indexName)
        createLuceneIndex(classType, indexName, indexName.split(".").last())
    }

    fun resetSbTreeIndex(classType: String, indexName: String): OIndex<*> {
        removeIndex(classType, indexName)
        return if (indexName.endsWith(".ic")) createICIndex(classType) else {
            val elems = indexName.split(".")
            val className = elems.first()
            val propertyName = elems.last()
            session(this) {
                return@session createBasicIndex(classProperty(className, propertyName))
            }
        }
    }

    fun countVertices(className: String) = query("select count() as c, @rid from $className") { result ->
        val data = result.toList()
        when (data.size) {
            0 -> 0
            else -> {
                val row = data[0]
                row.getProperty("c")
            }
        }
    }

    fun luceneIndexesOf(classType: String): List<OIndex<*>> = session(this) { session ->
        session.getClass(classType).classIndexes.filter { it.algorithm == "LUCENE" }
    }

    fun createICIndex(className: String): OIndex<*> {
        return session(this) { session ->
            session.command("CREATE INDEX $className.index.name.ic ON $className (name COLLATE ci) NOTUNIQUE")
            return@session session.getClass(className).getClassIndex("$className.index.name.ic")!!
        }
    }

    fun createBasicIndex(property: OProperty): OIndex<*> = property.createIndex(OClass.INDEX_TYPE.NOTUNIQUE)

    fun sbTreeIndexesOf(classType: String): List<OIndex<*>> = session(this) { session ->
        session.getClass(classType).classIndexes.filter { it.algorithm == "SBTREE" }
    }

    private fun classProperty(className: String, propertyName: String): OProperty = session(this) { session ->
        return@session session.getClass(className).getProperty(propertyName)
    }

    fun indexSize(index: OIndex<*>) = transaction(this) {
        return@transaction index.iterateEntriesMajor("", true, true).toKeys().size
    }

    fun getIndexes(): List<DBIndexInfo> = query("select expand(indexes) from metadata:indexmanager") { result ->
        val data = result.toList()
        data.map { vertex ->
            val indef = vertex.getProperty<OResultInternal>("indexDefinition")

            val indefMap = indef.propertyNames.map { it to indef.getProperty<String>(it) }.toMap()

            val metadata = if (vertex.hasProperty("metadata")) vertex.getProperty<OResultInternal>("metadata") else null

            val metadataMap = metadata?.let { md ->
                md.propertyNames.map { it to md.getProperty<String>(it) }.toMap()
            } ?: emptyMap()


            DBIndexInfo(
                name = vertex.getProperty("name"),
                type = vertex.getProperty("type"),
                algorithm = vertex.getProperty("algorithm"),
                indexVersion = vertex.getProperty("indexVersion"),
                valueContainerAlgorithm = vertex.getProperty("valueContainerAlgorithm"),
                indexDefinitionClass = vertex.getProperty("indexDefinitionClass"),
                clusters = vertex.getProperty("clusters"),
                indexDefinition = indefMap,
                metadata = metadataMap
            )
        }
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

class UniqueODatabaseSessionContainer(val session: ODatabaseSession) {
    val uuid = UUID.randomUUID()!!
}

fun ODatabaseSession.withUUID() = UniqueODatabaseSessionContainer(this)
