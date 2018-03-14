package com.infowings.catalog.search

import com.infowings.catalog.common.AspectsList
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
    fun measureSuggestion(commonParam: CommonSuggestionParam?, measureGroupName: String?): List<String> =
        suggestionService.findMeasure(commonParam, measureGroupName).map { it.name }

    /**
     * Полнотекстовый поиск по аспектам
     */
    @GetMapping("/aspect/suggestion")
    fun aspectSuggestion(
        context: SearchContext,
        commonParam: CommonSuggestionParam?,
        aspectParam: AspectSuggestionParam
    ): AspectsList = AspectsList(suggestionService.findAspect(context, commonParam, aspectParam))
}