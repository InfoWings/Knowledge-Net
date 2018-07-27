package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.aspects.getSuggestedAspects
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectsList
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeoutOrNull
import react.RBuilder

data class ShortAspectDescriptor(
    val id: String,
    val name: String,
    val subject: String?
)

private interface AspectOption : SelectOption {
    var aspectLabel: String
    var aspectDescriptor: ShortAspectDescriptor
}

private fun aspectOptionFromData(aspectData: AspectData) = jsObject<AspectOption> {
    this.aspectLabel = buildString {
        append(aspectData.name)
        append(" (")
        append(aspectData.subject?.name ?: "Global")
        append(")")
    }
    this.aspectDescriptor = ShortAspectDescriptor(
        aspectData.id ?: error("Aspect has no id"),
        aspectData.name,
        aspectData.subject?.name
    )
}

private fun aspectOptionFromDescriptor(aspectDescriptor: ShortAspectDescriptor) = jsObject<AspectOption> {
    this.aspectLabel = buildString {
        append(aspectDescriptor.name)
        append(" (")
        append(aspectDescriptor.subject ?: "Global")
        append(")")
    }
    this.aspectDescriptor = aspectDescriptor
}

fun RBuilder.propertyAspect(value: ShortAspectDescriptor?, onSelect: (ShortAspectDescriptor) -> Unit) =
    asyncSelect<AspectOption> {
        attrs {
            className = "object-property-input-aspect"
            placeholder = "Select Aspect"
            this.value = value?.let { aspectOptionFromDescriptor(it) }
            onChange = { onSelect(it.aspectDescriptor) }
            labelKey = "aspectLabel"
            valueKey = "aspectLabel"
            cache = false
            clearable = false
            options = emptyArray()
            filterOptions = { options, _, _ -> options }
            loadOptions = { input, callback ->
                if (input.isNotEmpty()) {
                    launch {
                        val aspectList: AspectsList? = withTimeoutOrNull(500) {
                            getSuggestedAspects(input)
                        }
                        callback(null, jsObject {
                            options = aspectList?.aspects?.map {
                                aspectOptionFromData(it)
                            }?.toTypedArray() ?: emptyArray()
                        })
                    }
                } else {
                    callback(null, jsObject {
                        options = emptyArray()
                    })
                }
                false
            }
        }
    }