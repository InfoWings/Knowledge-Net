package com.infowings.catalog.api

import com.infowings.catalog.common.AspectsList


interface AspectApi {
    fun getAspects(orderFields: List<String> = emptyList(), direct: List<String> = emptyList(), query: String? = null): AspectsList
}

