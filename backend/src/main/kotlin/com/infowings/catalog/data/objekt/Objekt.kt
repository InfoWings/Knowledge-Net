package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.ObjectGetResponse
import com.infowings.catalog.common.objekt.ObjectChangeResponse
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.id.ORID

/* Про ссылки на vertex-классы - см. комментарий в ObkectPropertyValue.kt */


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
    val properties: List<ObjectPropertyVertex>,
    val guid: String?
)

data class ObjectResult(private val objectVertex: ObjectVertex, private val subjectVertex: SubjectVertex) {

    private val guidValue = objectVertex.guid

    fun toResponse() = ObjectChangeResponse(
        objectVertex.id,
        objectVertex.name,
        objectVertex.description,
        subjectVertex.id,
        subjectVertex.name,
        objectVertex.version,
        guidValue
    )
}

data class ObjectTruncated(
    val id: ORID,
    val name: String,
    val guid: String?,
    val description: String?,
    val subjectName: String,
    val objectPropertiesCount: Int
) {
    fun toResponse() = ObjectGetResponse(id.toString(), name, guid, description, subjectName, objectPropertiesCount)
}

data class ObjectWriteInfo(val name: String, val description: String?, val subject: SubjectVertex)