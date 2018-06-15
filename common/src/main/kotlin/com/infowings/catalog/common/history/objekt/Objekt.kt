package com.infowings.catalog.common.history.objekt

import kotlinx.serialization.Serializable

class ObjectHistoryData {
    companion object {
        @Serializable
        data class Objekt(
            val id: String,
            val name: String,
            val description: String?,
            val subjectId: String,
            val subjectName: String
        )

        @Serializable
        data class Property(val id: String, val name: String, val aspectId: String, val aspectName: String)

        @Serializable
        data class Value(val id: String, val aspectPropertyId: String?, val aspectPropertyName: String?)

        @Serializable
        data class BriefState(val objekt: Objekt, val property: Property?, val value: Value?)
    }
}