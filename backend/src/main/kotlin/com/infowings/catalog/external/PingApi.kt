package com.infowings.catalog.external

import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class DbMetrics(val counts: Map<String, Int>, val indexCounts: Map<String, Int>, val edgeCounts: Map<String, Int>)

data class PingResponse(val pong: String, val details: String = "")

data class StatusResponse(val dbMetrics: DbMetrics, val indices: List<DBIndexInfo>)

@RestController
@RequestMapping("/api")
class PingApi(val db: OrientDatabase) {
    @GetMapping("/ping")
    fun ping(): PingResponse {
        logger.debug("Get ping request")

        return try {
            transaction(db) {
                // просто запрос к базе
                db.countVertices(ASPECT_CLASS)
                return@transaction PingResponse(pong = "OK")
            }
        } catch (e: Exception) {
            PingResponse(pong = "Database error", details = e.toString())
        }
    }

    @GetMapping("/status")
    fun status(): StatusResponse {
        logger.debug("Get status request")

        return transaction(db) {
            val indices = db.getIndexes()

            val counts = OrientClass.values().map {
                val className = it.extName
                className to db.countVertices(className)
            }.toMap()

            val edgeCounts = OrientEdge.values().map {
                val className = it.extName
                className to db.countVertices(className)
            }.toMap()

            val indicesByClass = indices.groupBy { it.nameElements()[0] }

            val indexedKeys = counts.keys.intersect(indicesByClass.keys)

            val indexCounts = indexedKeys.map {
                indicesByClass.getValue(it).mapNotNull { index ->
                    index.name to db.countVertices(index.name)
                }
            }.flatten().toMap()

            return@transaction StatusResponse(dbMetrics = DbMetrics(counts = counts, edgeCounts = edgeCounts, indexCounts = indexCounts), indices = indices)
        }
    }
}

private val logger = loggerFor<PingApi>()
