package com.infowings.catalog.api.impl

import com.infowings.catalog.api.AspectApi
import com.infowings.catalog.api.KNServerContext
import com.infowings.catalog.api.loggerFor
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectsList


internal class AspectApiWrapper(private val context: KNServerContext) : AspectApi {
    override fun getAspectById(id: String): AspectData {
        logger.debug("Get aspects by id [$id]request")

        return context.builder("/api/aspect/id")
            .path(id)
            .get()
    }

    override fun getAspects(orderFields: List<String>, direct: List<String>, query: String?): AspectsList {
        logger.debug("Get all aspects request, orderFields: ${orderFields.joinToString(",")}, direct: ${direct.joinToString(",")}, query: $query")

        return context.builder("/api/aspect/all")
            .parameter("orderFields", orderFields)
            .parameter("direct", direct)
            .parameter("q", query)
            .get()
    }
}

private val logger = loggerFor<AspectApi>()
