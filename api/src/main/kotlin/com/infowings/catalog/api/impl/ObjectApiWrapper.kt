package com.infowings.catalog.api.impl

import com.infowings.catalog.api.KNServerContext
import com.infowings.catalog.api.ObjectApi
import com.infowings.catalog.api.loggerFor
import com.infowings.catalog.common.objekt.*

internal class ObjectApiWrapper(private val context: KNServerContext) : ObjectApi {
    override fun createObject(request: ObjectCreateRequest): ObjectChangeResponse {
        logger.debug("Create object request: $request")

        return context.builder("/api/objects/create")
            .body(request)
            .post()
    }

    override fun createObjectProperty(request: PropertyCreateRequest): PropertyCreateResponse {
        logger.debug("Create object property request: $request")

        return context.builder("/api/objects/createProperty")
            .body(request)
            .post()
    }

    override fun createObjectValue(request: ValueCreateRequest): ValueChangeResponse {
        logger.debug("Create object property value request: $request")

        return context.builder("/api/objects/createValue")
            .body(request.toDTO())
            .post()
    }

    override fun updateObjectValue(request: ValueUpdateRequest): ValueChangeResponse {
        logger.debug("Create object property value request: $request")

        return context.builder("/api/objects/updateValue")
            .body(request.toDTO())
            .post()
    }

}

private val logger = loggerFor<ObjectApi>()
