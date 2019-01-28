package com.infowings.catalog.search

import com.infowings.catalog.common.*
import com.infowings.catalog.loggerFor
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
    ): SuggestedMeasureData {
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
        logger.debug("aspect suggestion request: context=$context commonParam=$commonParam aspectParam=$aspectParam")
        val aspects = suggestionService.findAspect(context, commonParam, aspectParam)
        return AspectsList(aspects, aspects.size)
    }

    @GetMapping("/aspect/hint")
    fun aspectHint(
        context: SearchContext,
        commonParam: CommonSuggestionParam?,
        aspectParam: AspectSuggestionParam
    ): AspectsHints {
        logger.debug("aspectHints request: context=$context commonParam=$commonParam aspectParam=$aspectParam")
        return suggestionService.aspectHints(context, commonParam, aspectParam)
    }

    /**
     * Fulltext search of subjects
     */
    @GetMapping("/subject/suggestion")
    fun subjectSuggestion(
        context: SearchContext,
        commonParam: CommonSuggestionParam?,
        subjectParam: SubjectSuggestionParam
    ): SubjectsList = SubjectsList(suggestionService.findSubject(commonParam, subjectParam))

    /**
     * Fulltext search of subjects
     */
    @GetMapping("/object/suggestion")
    fun objectSuggestion(
        context: SearchContext,
        commonParam: CommonSuggestionParam?,
        objectParam: ObjectSuggestionParam
    ): ObjectsList = ObjectsList(suggestionService.findObject(commonParam, objectParam))

}

private val logger = loggerFor<SearchController>()