package com.infowings.catalog.data.guid

import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex
import java.util.*

class GuidDaoService(private val db: OrientDatabase) {
    private val classEdgePairs = listOf(
        OrientClass.OBJECT_VALUE to OrientEdge.GUID_OF_OBJECT_VALUE
    )

    private val class2EdgeNames = classEdgePairs.map { it.first.extName to it.second.extName }.toMap()


    fun newGuidVertex(source: OVertex): GuidVertex {
        return transaction(db) {
            val res = db.createNewVertex(OrientClass.GUID.extName).toGuidVertex()
            res.guid = UUID.randomUUID().toString()
            val className = source.schemaType.get().name
            logger.debug("assign guid ${res.guid} to vertex ${source.id} of class $className")
            source.addEdge(res, class2EdgeNames[className] ?: throw IllegalStateException("Unexpected class name $className")).save<OEdge>()
            res.save<OVertex>()
            res
        }
    }

    fun find(guids: List<String>): List<GuidVertex> {
        return if (guids.isEmpty()) emptyList() else transaction(db) {
            db.query(
                "select from ${OrientClass.GUID.extName} where guid in :ids ", mapOf("ids" to guids)
            ) { rs ->
                rs.mapNotNull {
                    val v = it.toVertexOrNull()?.toGuidVertex()
                    v
                }.toList()
            }
        }
    }

    fun <T> findByGuidInClass(orientClass: OrientClass, guids: List<String>, block: (OVertex) -> T): List<T> {
        if (guids.isEmpty())
            return emptyList()

        return transaction(db) {
            db.query("select from ${orientClass.extName} where guid in :ids ", mapOf("ids" to guids)) { rs ->
                rs.mapNotNull { it.toVertexOrNull() }.map { block(it) }.toList()
            }
        }
    }

    fun findById(id: String): OVertex = transaction(db) { db[id] }

    fun vertex(guidVertex: GuidVertex): OVertex = guidVertex.getVertices(ODirection.IN).single()
}

private val logger = loggerFor<GuidDaoService>()
