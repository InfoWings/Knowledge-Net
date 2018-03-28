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
    fun measureSuggestion(
        commonParam: CommonSuggestionParam?,
        measureGroupName: String?,
        findInGroups: Boolean = false
    ): List<String> {
        logger.debug("measureSuggestion request: commonParam=$commonParam measureGroupName=$measureGroupName findInGroups=$findInGroups")
        return suggestionService.findMeasure(commonParam, measureGroupName, findInGroups)
    }

    /**
     * Fulltext search of aspects
     */
    @GetMapping("/aspect/suggestion")
    fun aspectSuggestion(
        context: SearchContext,
        commonParam: CommonSuggestionParam?,
        aspectParam: AspectSuggestionParam
    ): AspectsList {
        logger.debug("measureSuggestion request: context=$context commonParam=$commonParam aspectParam=$aspectParam")
        return AspectsList(suggestionService.findAspect(context, commonParam, aspectParam))
    }

    /**
     * Fulltext search of subjects
     */
    @GetMapping("/subject/suggestion")
    fun subjectSuggestion(
        context: SearchContext,
        commonParam: CommonSuggestionParam?,
        subjectParam: SubjectSuggestionParam
    ): SubjectsList = SubjectsList(suggestionService.findSubject(context, commonParam, subjectParam))

}

private val logger = loggerFor<SearchController>()