package com.infowings.catalog.storage

import com.infowings.catalog.common.objekt.EntityClass
import com.infowings.catalog.data.guid.toGuidVertex
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex


private val orient2Entity = mapOf(
    OrientClass.ASPECT to EntityClass.ASPECT,
    OrientClass.ASPECT_PROPERTY to EntityClass.ASPECT_PROPERTY,
    OrientClass.OBJECT to EntityClass.OBJECT,
    OrientClass.OBJECT_PROPERTY to EntityClass.OBJECT_PROPERTY,
    OrientClass.OBJECT_VALUE to EntityClass.OBJECT_VALUE,
    OrientClass.SUBJECT to EntityClass.SUBJECT,
    OrientClass.REFBOOK_ITEM to EntityClass.REFBOOK_ITEM
)

fun OVertex.entityClass(): EntityClass {
    return if (this.schemaType.isPresent) {
        orient2Entity.getValue(OrientClass.fromExtName(this.schemaType.get().name))
    } else throw IllegalStateException("No schema type")
}

fun OVertex.ofClass(orientClass: OrientClass): Boolean {
    return if (this.schemaType.isPresent) {
        this.schemaType.get().name == orientClass.extName
    } else false
}

fun OVertex.checkClass(orientClass: OrientClass) {
    if (!this.ofClass(orientClass)) throw IllegalStateException("vertex with id ${this.id} is of class ${this.schemaType}")
}

fun OVertex.checkClassAny(orientClasses: List<OrientClass>) {
    if (!orientClasses.any { this.ofClass(it) }) throw IllegalStateException("vertex with id ${this.id} is of class ${this.schemaType}")
}

fun OVertex.guid(edge: OrientEdge): String? = getVertices(ODirection.OUT, edge.extName).singleOrNull()?.toGuidVertex()?.guid