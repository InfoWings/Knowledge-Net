package com.infowings.catalog.storage

import com.orientechnologies.orient.core.db.ODatabasePool
import com.orientechnologies.orient.core.db.ODatabaseSession
import com.orientechnologies.orient.core.db.OrientDB
import com.orientechnologies.orient.core.db.OrientDBConfig
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.tx.OTransaction
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import javax.annotation.PreDestroy


class OrientDatabase(url: String, database: String, user: String, password: String) {

    companion object {
        private val logger = LoggerFactory.getLogger(OrientDatabase::class.java)
    }

    private var orientDB = OrientDB(url, user, password, OrientDBConfig.defaultConfig())
    private var dbPool = ODatabasePool(orientDB, database, "admin", "admin")

    fun acquire(): ODatabaseDocument = dbPool.acquire()

    init {
        // создаем необходимые классы
        val session = dbPool.acquire()
        runStartupScript("startup.sql", session)
        //session.createClassIfNotExist("Aspect")
        session.close()
    }

    @PreDestroy
    fun cleanUp() {
        dbPool.close()
        orientDB.close()
    }

    private fun runStartupScript(filename: String, session: ODatabaseSession) {
        val classLoader = javaClass.classLoader
        val scriptFile = Paths.get(classLoader.getResource(filename)!!.toURI())
        if (scriptFile.toFile().exists()) {
            logger.info("Executing script $filename")
            val script = Files.newBufferedReader(scriptFile).use {
                return@use it.readText()
            }
            try {
                session.execute("sql", script).close()
            } catch (ignored: Exception) {
            }
        }
    }
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