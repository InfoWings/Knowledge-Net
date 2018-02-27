package com.infowings.catalog.components

import com.infowings.catalog.common.MeasureGroupMap
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.commonSelect
import kotlinext.js.jsObject
import react.*

private interface Option : SelectOption {
    var measure: String
}

private fun measureOption(name: String) = jsObject<Option> {
    measure = name
}

class SuggestingInput : RComponent<SuggestingInput.Props, SuggestingInput.State>() {

    override fun RBuilder.render() {
        commonSelect<Option> {
            attrs {
                value = props.initialValue
                labelKey = "measure"
                valueKey = "measure"
                options = MeasureGroupMap.flatMap { it.value.measureList }.map { measureOption(it.name) }.toTypedArray()
                onChange = { props.onOptionSelected(it.measure) }
            }
        }
    }

    interface Props : RProps {
        var initialValue: String
        var onOptionSelected: (String) -> Unit
    }

    interface State : RState
}