package com.infowings.catalog.api.impl

import com.infowings.catalog.api.KNServerContext
import com.infowings.catalog.api.ObjectApi
import com.infowings.catalog.api.loggerFor
import com.infowings.catalog.common.DetailedObjectViewResponse
import com.infowings.catalog.common.DetailedObjectViewResponseList
import com.infowings.catalog.common.ObjectEditDetailsResponse
import com.infowings.catalog.common.ObjectsResponse
import com.infowings.catalog.common.objekt.*

internal class ObjectApiWrapper(private val context: KNServerContext) : ObjectApi {

    override fun getDetailedObject(id: String): DetailedObjectViewResponse {
        logger.debug("Get object {$id} request")

//        return context.builder("/api/objects").path("$id/viewdetails").get()
        return context.builder("/api/objects").path("${id}/viewdetails").get()
    }

    override fun getAllDetailedObject(): DetailedObjectViewResponseList {
        return context.builder("/api/objects/viewdetails").get()
    }

    //    @GetMapping("{id}/editdetails")
    override fun getDetailedObjectForEdit(id: String): ObjectEditDetailsResponse {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllObjects(): ObjectsResponse {
        logger.debug("Get all objects request")

        return context.builder("/api/objects").get()
    }

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
