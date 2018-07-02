package com.infowings.catalog.objects.treeedit.inputs

import com.infowings.catalog.aspects.getSuggestedAspects
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectsList
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeoutOrNull
import react.RBuilder

private interface AspectOption : SelectOption {
    var aspectLabel: String
    var aspectData: AspectData
}

private fun aspectOption(aspectData: AspectData) = jsObject<AspectOption> {
    this.aspectLabel = buildString {
        append(aspectData.name ?: error("Inconsistent State"))
        append(" (")
        append(aspectData.subject?.name ?: "Global")
        append(")")
    }
    this.aspectData = aspectData
}

fun RBuilder.propertyAspect(value: AspectData?, onSelect: (AspectData) -> Unit, onOpen: () -> Unit) =
    asyncSelect<AspectOption> {
        attrs {
            className = "object-property-input-aspect"
            placeholder = "Select Aspect"
            this.value = value?.let {
                buildString {
                    append(it.name ?: error("Inconsistent State"))
                    append(" (")
                    append(it.subject?.name ?: "Global")
                    append(")")
                }
            } ?: ""
            onChange = { onSelect(it.aspectData) }
            labelKey = "aspectLabel"
            valueKey = "aspectLabel"
            cache = false
            clearable = false
            options = value?.let { arrayOf(aspectOption(it)) } ?: emptyArray()
            this.onOpen = onOpen
            loadOptions = { input, callback ->
                if (input.isNotEmpty()) {
                    launch {
                        val aspectList: AspectsList? = withTimeoutOrNull(500) {
                            getSuggestedAspects(input)
                        }
                        callback(null, jsObject {
                            options = aspectList?.aspects?.map {
                                aspectOption(it)
                            }?.toTypedArray() ?: emptyArray()
                        })
                    }
                } else {
                    callback(null, jsObject {
                        options = value?.let { arrayOf(aspectOption(it)) } ?: emptyArray()
                    })
                }
                false
            }
        }
    }