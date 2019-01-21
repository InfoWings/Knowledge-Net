package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.aspects.getAspectHints
import com.infowings.catalog.aspects.listEntry
import com.infowings.catalog.common.AspectHint
import com.infowings.catalog.common.AspectsHints
import com.infowings.catalog.wrappers.react.asReactElement
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeoutOrNull
import react.RBuilder
import react.ReactElement

private interface AspectOption : SelectOption {
    var aspectLabel: String
    var aspectEntry: ReactElement
    var aspectHint: AspectHint
}

private fun aspectOption(aspectHint: AspectHint) = jsObject<AspectOption> {
    this.aspectLabel = aspectHint.name
    this.aspectHint = aspectHint
    this.aspectEntry = aspectHint.listEntry()
}

private fun aspectOptionSelected(aspectHint: AspectHint) = jsObject<AspectOption> {
    this.aspectLabel = aspectHint.name
    this.aspectHint = aspectHint
    this.aspectEntry = aspectHint.name.asReactElement()
}


fun RBuilder.propertyAspect(value: AspectHint?,
                            onSelect: (AspectHint) -> Unit,
                            onActivity: () -> Unit,
                            disabled: Boolean = false) =
    asyncSelect<AspectOption> {
        attrs {
            className = "object-property-input-aspect"
            placeholder = "Select Aspect"
            this.value =  value?.let {
                aspectOptionSelected(it)
            }
            onChange = {
                onSelect(it.aspectHint)
            }
            labelKey = "aspectEntry"
            cache = false
            clearable = false
            options = emptyArray()
            filterOptions = { options, _, _ -> options }
            loadOptions = { input, callback ->
                if (input.isNotEmpty()) {
                    launch {
                        val hints: AspectsHints? = withTimeoutOrNull(500) {
                            getAspectHints(input)
                        }
                        callback(null, jsObject {
                            options = (hints?.defaultOrder()?.map {
                                aspectOption(it)
                            } ?: emptyList()).toTypedArray()
                        })
                    }
                    onActivity()
                } else {
                    callback(null, jsObject {
                        options = emptyArray()
                    })
                }
                false
            }
            this.disabled = disabled
        }
    }