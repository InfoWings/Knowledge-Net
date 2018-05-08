package com.infowings.catalog.objects

import com.infowings.catalog.common.*

data class ObjectViewModel(
    val id: String?,
    var name: String?,
    var subject: SubjectData?,
    var properties: MutableList<ObjectPropertyViewModel>,
    var expanded: Boolean = false
) {
    fun toObjectData() =
        ObjectData(id, name, subject ?: error("Inconsistent State"), properties.map { it.toObjectPropertyData() })
}

data class ObjectPropertyViewModel(
    val id: String? = null,
    var name: String? = null,
    var cardinality: Cardinality? = null,
    var aspect: AspectData? = null,
    var values: MutableList<ObjectPropertyValueViewModel>? = null,
    var expanded: Boolean = true
) {
    fun toObjectPropertyData() = ObjectPropertyData(
        id,
        name,
        cardinality?.name ?: error("Inconsistent State"),
        aspect ?: error("Inconsistent State"),
        values?.map {
            ObjectPropertyValueData(
                it.id,
                it.value ?: error("Inconsistent State"),
                emptyList()
            )
        } ?: emptyList()
    )

}

data class ObjectPropertyValueViewModel(
    val id: String? = null,
    var value: String? = null,
    var expanded: Boolean = false,
    var valueGroups: MutableList<AspectPropertyValueGroupViewModel> = ArrayList()
)

data class AspectPropertyValueGroupViewModel(
    val property: AspectPropertyViewModel,
    var values: MutableList<AspectPropertyValueViewModel> = ArrayList()
)

data class AspectPropertyValueViewModel(
    val id: String? = null,
    var value: String? = null,
    var expanded: Boolean = false,
    var children: MutableList<AspectPropertyValueGroupViewModel> = ArrayList()
)

data class AspectPropertyViewModel(
    val propertyId: String,
    val aspectId: String,
    val cardinality: Cardinality,
    val roleName: String?,
    val aspectName: String,
    val baseType: String,
    val domain: String
)

enum class Cardinality {
    ZERO, ONE, INFINITY
}
