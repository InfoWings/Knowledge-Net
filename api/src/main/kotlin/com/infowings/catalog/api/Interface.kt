package com.infowings.catalog.api

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.*
import org.springframework.web.bind.annotation.PathVariable

interface SubjectApi {
    fun createSubject(subjectData: SubjectData): SubjectData
    fun getSubject(): SubjectsList
    fun getSubjectByName(subjectName: String): SubjectData?
    fun getSubjectById(id: String): SubjectData?
    fun updateSubject(subjectData: SubjectData): SubjectData
    fun removeSubject(subjectData: SubjectData)
    fun forceRemoveSubject(subjectData: SubjectData)
}

interface AspectApi {
    fun getAspects(orderFields: List<String> = emptyList(), direct: List<String> = emptyList(), query: String? = null): AspectsList
    fun getAspectById(@PathVariable id: String): AspectData?
}

interface ObjectApi {
    fun createObject(request: ObjectCreateRequest): ObjectChangeResponse
    fun createObjectProperty(request: PropertyCreateRequest): PropertyCreateResponse
    fun createObjectValue(request: ValueCreateRequest): ValueChangeResponse
    fun updateObjectValue(request: ValueUpdateRequest): ValueChangeResponse

    fun getAllObjects(): ObjectsResponse
    fun getDetailedObject(id: String): DetailedObjectViewResponse
    fun getDetailedObjectForEdit(id: String): ObjectEditDetailsResponse

    fun getAllDetailedObject(): DetailedObjectViewResponseList
}