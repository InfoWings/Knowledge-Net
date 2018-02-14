package com.infowings.catalog.search

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST API поиска
 */
@RestController
@RequestMapping("/api/search")
class SearchController {

    @Autowired
    lateinit var suggestionService: SuggestionService

    /**
     * Полнотекстовый поиск по измеряемым величинам и единицам измерения
     */
    @GetMapping("/measure/suggestion")
    fun find(user: String, text: String) : List<MeasureSuggestionDto> = suggestionService.find(user, text)
}