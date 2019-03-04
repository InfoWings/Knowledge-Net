package com.infowings.catalog.data.guid

import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex

class GuidDaoService(private val db: OrientDatabase) {

    fun <T> findByGuidInClass(orientClass: OrientClass, guids: List<String>, block: (OVertex) -> T): List<T> {
        if (guids.isEmpty())
            return emptyList()

        return transaction(db) {
            db.query("select from ${orientClass.extName} where guid in :ids ", mapOf("ids" to guids)) { rs ->
                rs.mapNotNull { it.toVertexOrNull() }.map { block(it) }.toList()
            }
        }
    }

    fun findById(id: String): OVertex = session(db) { db[id] }

}