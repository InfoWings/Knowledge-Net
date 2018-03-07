package com.infowings.catalog.search

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectsList
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
    fun measureSuggestion(context: SearchContext, @RequestParam text: String) =
        suggestionService.findMeasure(context, text).map { it.name }

    /**
     * Полнотекстовый поиск по аспектам
     */
    @GetMapping("/aspect/suggestion")
    fun aspectSuggestion(context: SearchContext, @RequestParam text: String): AspectsList =
            AspectsList(suggestionService.findAspect(context, text))
}