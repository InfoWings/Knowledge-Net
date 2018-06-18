package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.ObjectGetResponse
import com.infowings.catalog.data.subject.SubjectVertex
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
    val properties: List<ObjectPropertyVertex>
)

data class ObjectTruncated(
    val id: ORID,
    val name: String,
    val description: String?,
    val subjectId: ORID,
    val subjectName: String,
    val subjectDescription: String?
) {
    fun toResponse() = ObjectGetResponse(id.toString(), name, description, subjectId.toString(), subjectName, subjectDescription)
}

data class ObjectCreateInfo(val name: String, val description: String?, val subject: SubjectVertex)