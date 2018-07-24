package com.infowings.catalog.external

import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/support")
class SupportApi(val db: OrientDatabase) {
    val classNames = OrientClass.values().map { it.extName }.toSet()

    private fun reset(index: DBIndexInfo) {
        when (index.algorithm) {
            "LUCENE" -> {
                logger.info("lucene index $index")
                db.resetLuceneIndex(index.nameElements()[0], index.name)
            }
            "SBTREE" -> {
                logger.info("sbtree index $index")
                db.resetSbTreeIndex(index.nameElements()[0], index.name)
            }
            else -> {
                logger.info("unknown algorithm of index $index. Skip")
            }
        }
    }

    @PutMapping("/reset-index/{name}")
    fun resetIndex(@PathVariable name: String) {
        logger.debug("Get reset index $name request")
        db.getIndexes().find { it.name == name }?.let { reset(it) } ?: throw IllegalArgumentException("index not found: $name")
    }

    @PutMapping("/reset-indexes")
    fun resetIndexes() {
        logger.debug("Get reset indexes request")
        db.getIndexes().filter { classNames.contains(it.nameElements()[0]) }.forEach { reset(it) }
    }
}

private val logger = loggerFor<SupportApi>()