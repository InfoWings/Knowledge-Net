package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.ObjectData
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.id.ORID

/**
 * Object data representation for use in backend context.
 * It can use Orient data structures but it is detached from database - updated to it does not lead to
 * updates in database
 */
data class Objekt(
    val id: ORID?,
    val name: String,
    val description: String?,
    val subject: SubjectVertex,
    val properties: List<ObjectPropertyVertex>
) {
    fun toObjectData(): ObjectData = ObjectData(id?.toString(), name, description, subject.id, properties.map {it.id})
}
