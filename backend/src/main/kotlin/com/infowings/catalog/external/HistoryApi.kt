package com.infowings.catalog.external

import com.infowings.catalog.common.AspectHistoryList
import com.infowings.catalog.data.history.providers.AspectHistoryProvider
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/history")
class HistoryApi(val aspectHistoryProvider: AspectHistoryProvider) {

    @GetMapping("aspects")
    fun getAspects(): AspectHistoryList = AspectHistoryList(aspectHistoryProvider.getAllHistory())
}