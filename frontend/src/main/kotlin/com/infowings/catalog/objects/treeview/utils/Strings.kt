package com.infowings.catalog.objects.treeview.utils

import com.infowings.catalog.common.AspectData
import react.RBuilder
import react.dom.span

fun RBuilder.propertyAspectTypeInfo(value: AspectData?) = value?.let {
    span(classes = "property-aspect-type") {
        +(it.domain ?: it.baseType ?: error("Inconsistent State"))
    }
}
