package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncCreatableSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState

interface Option : SelectOption {
    var aspectData: AspectData
    var aspectName: String
    var aspectLabel: String
}

fun aspectOption(data: AspectData) = jsObject<Option> {
    aspectLabel = "${data.name} ${data.measure?.let { "(${data.measure})" } ?: ""}"
    aspectName = data.name ?: ""
    aspectData = data
}

fun renameAspectOption(data: AspectData, newAspectName: String) = jsObject<Option> {
    aspectLabel = "$newAspectName ${data.measure?.let { "(${data.measure})" } ?: ""}"
    aspectName = newAspectName
    aspectData = data
}

class AspectSuggestingInput : RComponent<AspectSuggestingInput.Props, AspectSuggestingInput.State>() {

    override fun RBuilder.render() {
        asyncCreatableSelect<Option> {
            attrs {
                className = "aspect-table-select"
                value = props.associatedAspect.name ?: ""
                labelKey = "aspectLabel"
                valueKey = "aspectName"
                onChange = { props.onOptionSelected(it.aspectData) }
                cache = false
                clearable = false
                options = arrayOf(aspectOption(props.associatedAspect))
                loadOptions = { input, callback ->
                    if (input.isNotEmpty()) {
                        launch {
                            val searchResult = getSuggestedAspects(input)
                            callback(null, jsObject {
                                options = searchResult.aspects.map { aspectOption(it) }.toTypedArray()
                            })
                        }
                    } else {
                        callback(null, jsObject {
                            options = arrayOf(aspectOption(props.associatedAspect))
                        })
                    }
                    false // Hack to not return Unit from the function that is considered true if placed in `if (Unit)` in javascript
                }
                promptTextCreator = { "Change aspect name to $it" }
                newOptionCreator = { renameAspectOption(props.associatedAspect, it.label) }
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