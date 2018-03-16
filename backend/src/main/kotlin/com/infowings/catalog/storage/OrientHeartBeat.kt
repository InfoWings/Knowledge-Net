package com.infowings.catalog.storage

import com.infowings.catalog.loggerFor
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.TimeUnit

private val logger = loggerFor<OrientDatabase>()
private const val QUERY = "SELECT * from User where username = ?"
private const val USER = "username"

/**
 * Периодически шлем запросы, чтобы сессия не закрывалась
 */
class OrientHeartBeat(database: OrientDatabase, seconds: Int) {
    init {
        val period = TimeUnit.SECONDS.toMillis(seconds.toLong());

        logger.info("Initializing orient heart beat with $seconds seconds period")

        launch {
            while (isActive) {
                delay(period)
                database.query(QUERY, USER) { logger.trace("heart beat result: $it") }
            }
        }

    }
}
