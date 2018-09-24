package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.objekt.ObjectChangeResponse
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.data.Subject

class ObjectBuilder(val objectService: ObjectService) {
    private var name: String? = null
    private var description: String? = null
    private var subjectId: String? = null

    private fun <T> strict(value: T?, propName: String) = value ?:throw IllegalStateException("$propName is not defined")

    fun name(value: String): ObjectBuilder {
        this.name = value
        return this
    }

    fun description(value: String): ObjectBuilder {
        this.description = value
        return this
    }

    fun subject(id: String): ObjectBuilder {
        this.subjectId = id
        return this
    }

    fun subject(subject: Subject): ObjectBuilder {
        this.subjectId = subject.id
        return this
    }

    fun build(): ObjectChangeResponse {
        val objectCreateRequest = ObjectCreateRequest(strict(name, "object name"), description, strict(subjectId, "subject id"))
        return objectService.create(objectCreateRequest, "admin")
    }
}