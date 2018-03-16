package com.infowings.catalog.storage

import com.infowings.catalog.loggerFor
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.TimeUnit

/**
 * Периодически шлем запросы, чтобы сессия не закрывалась
 */
class OrientHeartBeat(database: OrientDatabase, seconds: Int) {
    init {
        val logger = loggerFor<OrientDatabase>()

        val period = TimeUnit.SECONDS.toMillis(seconds.toLong());

        logger.info("Initializing orient heart beat with $seconds seconds period")

        launch {
            while (isActive) {
                delay(period)
                val res = session(database) {
                    val query = "SELECT * from User where username = ?"
                    it.query(query, "username")
                }
                logger.info("heart beat result: ${res}")
            }
        }

    }
}
