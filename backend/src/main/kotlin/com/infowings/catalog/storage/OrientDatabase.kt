package com.infowings.catalog.storage

import com.orientechnologies.orient.core.db.ODatabasePool
import com.orientechnologies.orient.core.db.ODatabaseSession
import com.orientechnologies.orient.core.db.OrientDB
import com.orientechnologies.orient.core.db.OrientDBConfig
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.tx.OTransaction
import org.slf4j.LoggerFactory
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
        dbPool.acquire().use {
            runStartupScript(it)
        }
    }

    @PreDestroy
    fun cleanUp() {
        dbPool.close()
        orientDB.close()
    }

    private fun runStartupScript(session: ODatabaseSession) {
        session.createClassIfNotExist("Aspect")
        if (session.getClass("User") == null) {
            val userClass = session.createClass("User")
            userClass.createProperty("username", OType.STRING).createIndex(OClass.INDEX_TYPE.UNIQUE)
            userClass.createProperty("password", OType.STRING)
            userClass.createProperty("role", OType.STRING)

            val user: OElement = session.newInstance("User")
            user.setProperty("username", "user")
            user.setProperty("password", "user")
            user.setProperty("role", "USER")
            user.save<ORecord>()

            val admin: OElement = session.newInstance("User")
            admin.setProperty("username", "admin")
            admin.setProperty("password", "admin")
            admin.setProperty("role", "ADMIN")
            admin.save<ORecord>()

            val poweredUser: OElement = session.newInstance("USER")
            poweredUser.setProperty("username", "powereduser")
            poweredUser.setProperty("password", "powereduser")
            poweredUser.setProperty("role", "POWERED_USER")
            poweredUser.save<ORecord>()
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