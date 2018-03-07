package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.wrappers.select.*
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState

interface Option : SelectOption {
    var aspectData: AspectData
    var aspectName: String
}

fun aspectOption(data: AspectData, name: String) = jsObject<Option> {
    aspectName = name
    aspectData = data
}

class AspectSuggestingInput : RComponent<AspectSuggestingInput.Props, AspectSuggestingInput.State>() {

    override fun RBuilder.render() {
        asyncCreatableSelect<Option> {
            attrs {
                className = "aspect-table-select"
                value = props.associatedAspect.name
                labelKey = "aspectName"
                valueKey = "aspectName"
                onChange = { props.onOptionSelected(it.aspectData) }
                cache = false
                clearable = false
                options = arrayOf(aspectOption(props.associatedAspect, props.associatedAspect.name))
                loadOptions = { input, callback ->
                    if (input.isNotEmpty()) {
                        launch {
                            val searchResult = getSuggestedAspects(input)
                            callback(null, jsObject {
                                options = searchResult.aspects.map { aspectOption(it, it.name) }.toTypedArray()
                            })
                        }
                    } else {
                        callback(null, jsObject {
                            options = arrayOf(aspectOption(props.associatedAspect, props.associatedAspect.name))
                        })
                    }
                    false // Hack to not return Unit from the function that is considered true if placed in `if (Unit)` in javascript
                }
                promptTextCreator = { "Change aspect name to $it" }
                newOptionCreator = { aspectOption(props.associatedAspect, it.label) }
                onNewOptionClick = { props.onAspectNameChanged(it.aspectData, it.aspectName) }
            }
        }
    }

    interface Props : RProps {
        var associatedAspect: AspectData
        var onOptionSelected: (AspectData) -> Unit
        var onAspectNameChanged: (AspectData, String) -> Unit
    }

    interface State : RState
}