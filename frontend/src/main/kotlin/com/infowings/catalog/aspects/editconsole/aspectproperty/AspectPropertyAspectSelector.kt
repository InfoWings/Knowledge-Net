package com.infowings.catalog.aspects.editconsole.aspectproperty

import com.infowings.catalog.aspects.getSuggestedAspects
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.wrappers.react.label
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import react.*
import react.dom.div

interface AspectOption : SelectOption {
    var aspectData: AspectData
    var aspectName: String
    var aspectLabel: String
}

fun aspectOption(data: AspectData) = jsObject<AspectOption> {
    aspectLabel = "${data.name} ${data.measure?.let { "(${data.measure})" } ?: ""}"
    aspectName = data.name ?: ""
    aspectData = data
}

class AspectPropertyAspectSelector : RComponent<AspectPropertyAspectSelector.Props, RState>() {

    private fun handleSelectAspectOption(option: AspectOption) {
        props.onAspectSelected(option.aspectData)
    }

    override fun RBuilder.render() {
        val boundAspect = props.aspect
        div(classes = "aspect-edit-console--aspect-property-input-container") {
            label(classes = "aspect-edit-console--input-label") {
                +"Aspect"
            }
            div(classes = "aspect-edit-console--input-wrapper") {
                asyncSelect<AspectOption> {
                    attrs {
                        className = "aspect-table-select"
                        value = boundAspect?.name ?: ""
                        labelKey = "aspectLabel"
                        valueKey = "aspectName"
                        onChange = ::handleSelectAspectOption
                        cache = false
                        clearable = false
                        options = if (boundAspect == null) emptyArray() else arrayOf(aspectOption(boundAspect))
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
                                    options = if (boundAspect == null) emptyArray()
                                    else arrayOf(aspectOption(boundAspect))
                                })
                            }
                            false // Hack to not return Unit from the function that is considered true if placed in `if (Unit)` in javascript
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspect: AspectData?
        var onAspectSelected: (AspectData) -> Unit
    }
}

fun RBuilder.aspectPropertyAspect(block: RHandler<AspectPropertyAspectSelector.Props>) = child(AspectPropertyAspectSelector::class, block)