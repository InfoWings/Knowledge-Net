package com.infowings.catalog.data.guid

import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex
import java.util.*

class GuidDaoService(private val db: OrientDatabase) {
    private val classEdgePairs = listOf(
        OrientClass.ASPECT to OrientEdge.GUID_OF_ASPECT,
        OrientClass.ASPECT_PROPERTY to OrientEdge.GUID_OF_ASPECT_PROPERTY,
        OrientClass.SUBJECT to OrientEdge.GUID_OF_SUBJECT,
        OrientClass.REFBOOK_ITEM to OrientEdge.GUID_OF_REFBOOK_ITEM,
        OrientClass.OBJECT to OrientEdge.GUID_OF_OBJECT,
        OrientClass.OBJECT_PROPERTY to OrientEdge.GUID_OF_OBJECT_PROPERTY,
        OrientClass.OBJECT_VALUE to OrientEdge.GUID_OF_OBJECT_VALUE
    )

    private val class2EdgeNames = classEdgePairs.map { it.first.extName to it.second.extName }.toMap()
    val edge2Class: Map<OrientEdge, OrientClass> = classEdgePairs.map { it.second to it.first }.toMap()



    fun newGuidVertex(source: OVertex): GuidVertex {
        return transaction (db) {
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
        return if (guids.isEmpty()) emptyList() else transaction (db) {
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

    fun ofAspects(aspectIds: List<ORID>): Map<String, String> {
        return if (aspectIds.isEmpty()) emptyMap() else transaction (db) {
            db.query(
                "select @rid, out('${OrientEdge.GUID_OF_ASPECT.extName}').guid as guid from ${OrientClass.ASPECT.extName}" +
                        " where (deleted = false or deleted is null) and @rid in :ids ", mapOf("ids" to aspectIds)
            ) { rs ->
                rs.mapNotNull { oResult ->
                    val guid = oResult.getProperty<List<String>>("guid").single()
                    val rid = oResult.getProperty<ORID>("@rid").toString()
                    rid to guid
                }.toMap()
            }
        }
    }

    fun findById(id: String): OVertex = transaction(db) { db[id] }

    fun vertex(guidVertex: GuidVertex): OVertex = guidVertex.getVertices(ODirection.IN).single()
}

private val logger = loggerFor<GuidDaoService>()
