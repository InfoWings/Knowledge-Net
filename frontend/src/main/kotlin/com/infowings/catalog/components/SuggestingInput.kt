package com.infowings.catalog.components

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.commonSelect
import kotlinext.js.jsObject
import react.*

interface Option : SelectOption {
    var aspectData: AspectData
    var aspectName: String
}

fun aspectOption(data: AspectData, name: String) = jsObject<Option> {
    aspectName = name
    aspectData = data
}

class SuggestingInput : RComponent<SuggestingInput.Props, SuggestingInput.State>() {

    override fun RBuilder.render() {
        commonSelect<Option> {
            attrs {
                value = props.initialValue
                labelKey = "aspectName"
                valueKey = "aspectName"
                options = props.options
                onChange = { props.onOptionSelected(it.aspectData) }
            }
        }
    }

    interface Props : RProps {
        var initialValue: String
        var options: Array<Option>
        var onOptionSelected: (AspectData) -> Unit
    }

    interface State : RState
}