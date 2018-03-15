package com.infowings.catalog.storage

import com.infowings.catalog.loggerFor
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

/**
 * Периодически шлем запросы, чтобы сеанс не закрывался
 */
class OrientHeartBeat(database: OrientDatabase, seconds: Int) {
    init {
        val logger = loggerFor<OrientDatabase>()

        val period = seconds * 1000;

        logger.info("Initializing orient heart beat...")

        launch {
            while (true) {
                delay(period)
                val res = transaction(database) {
                    val query = "SELECT * from User where username = ?"
                    it.query(query, "username")
                }
                logger.info("heart beat result result: ${res}")
            }
        }

    }
}
