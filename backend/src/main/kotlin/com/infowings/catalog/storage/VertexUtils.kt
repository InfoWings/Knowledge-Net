package com.infowings.catalog.storage

import com.infowings.catalog.common.guid.EntityClass
import com.infowings.catalog.data.aspect.toAspectPropertyVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.objekt.toObjectPropertyValueVertex
import com.infowings.catalog.data.objekt.toObjectPropertyVertex
import com.infowings.catalog.data.objekt.toObjectVertex
import com.infowings.catalog.data.reference.book.toReferenceBookItemVertex
import com.infowings.catalog.data.subject.toSubjectVertex
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex
import java.util.*


private val orient2Entity = mapOf(
    OrientClass.ASPECT to EntityClass.ASPECT,
    OrientClass.ASPECT_PROPERTY to EntityClass.ASPECT_PROPERTY,
    OrientClass.OBJECT to EntityClass.OBJECT,
    OrientClass.OBJECT_PROPERTY to EntityClass.OBJECT_PROPERTY,
    OrientClass.OBJECT_VALUE to EntityClass.OBJECT_VALUE,
    OrientClass.SUBJECT to EntityClass.SUBJECT,
    OrientClass.REFBOOK_ITEM to EntityClass.REFBOOK_ITEM
)

fun OVertex.asHistoryAware(): HistoryAware = when (orientClass()) {
    OrientClass.ASPECT -> toAspectVertex()
    OrientClass.ASPECT_PROPERTY -> toAspectPropertyVertex()
    OrientClass.OBJECT -> toObjectVertex()
    OrientClass.OBJECT_PROPERTY -> toObjectPropertyVertex()
    OrientClass.OBJECT_VALUE -> toObjectPropertyValueVertex()
    OrientClass.SUBJECT -> toSubjectVertex()
    OrientClass.REFBOOK_ITEM -> toReferenceBookItemVertex()
    else -> throw IllegalStateException("Unknown class")

}

fun OVertex.deleteOutEdges(edgeClass: OrientEdge) = getEdges(ODirection.OUT, edgeClass.extName).forEach { it.delete<OEdge>() }


fun OVertex.orientClass(): OrientClass {
    return if (this.schemaType.isPresent) {
        OrientClass.fromExtName(this.schemaType.get().name)
    } else throw IllegalStateException("No schema type")
}

fun OVertex.assignGuid(): OVertex {
    setProperty(ATTR_GUID, UUID.randomUUID().toString())
    return this
}

fun OVertex.entityClass(): EntityClass = orient2Entity.getValue(orientClass())

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
