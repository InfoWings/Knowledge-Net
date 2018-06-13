package com.infowings.catalog.external

import com.infowings.catalog.common.AspectHistoryList
import com.infowings.catalog.common.RefBookHistoryList
import com.infowings.catalog.data.history.providers.AspectHistoryProvider
import com.infowings.catalog.data.history.providers.RefBookHistoryProvider
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/history")
class HistoryApi(val aspectHistoryProvider: AspectHistoryProvider, val refBookHistoryProvider: RefBookHistoryProvider) {

    @GetMapping("aspects")
    fun getAspects(): AspectHistoryList = AspectHistoryList(aspectHistoryProvider.getAllHistory())

    @GetMapping("refbook")
    fun getRefBooks() = RefBookHistoryList(refBookHistoryProvider.getAllHistory())
}