package com.infowings.catalog.search

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.Measure
import com.infowings.common.search.SearchContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST API поиска
 */
@RestController
@RequestMapping("/api/search")
class SearchController(val suggestionService: SuggestionService) {

    /**
     * Полнотекстовый поиск по измеряемым величинам и единицам измерения
     */
    @GetMapping("/measure/suggestion")
    fun measureSuggestion(context: SearchContext, text: String): List<Measure<*>> =
            suggestionService.findMeasure(context, text)

    /**
     * Полнотекстовый поиск по аспектам
     */
    @GetMapping("/aspect/suggestion")
    fun aspectSuggestion(context: SearchContext, text: String): List<AspectData> =
            suggestionService.findAspect(context, text)
}