package com.infowings.catalog.aspects.editconsole.aspectproperty

import com.infowings.catalog.aspects.getSuggestedAspects
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.wrappers.react.label
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import react.*
import react.dom.div
import react.dom.span

interface AspectOption : SelectOption {
    var aspectData: AspectData
    var aspectLabel: String
}

fun aspectOption(data: AspectData) = jsObject<AspectOption> {
    aspectLabel = "${data.name} ${data.subject?.let { "(${it.name})" } ?: "(Global)"}"
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
                        value = boundAspect?.let {
                            if (it.name != null) "${it.name} ${it.subject?.let { "(${it.name})" }
                                    ?: "(Global)"}" else ""
                        } ?: ""
                        labelKey = "aspectLabel"
                        valueKey = "aspectLabel"
                        onChange = ::handleSelectAspectOption
                        cache = false
                        clearable = false
                        options = if (boundAspect == null) emptyArray() else arrayOf(aspectOption(boundAspect))
                        loadOptions = { input, callback ->
                            if (input.isNotEmpty()) {
                                launch {
                                    val searchResult = getSuggestedAspects(
                                        input,
                                        props.parentAspectId,
                                        props.aspectPropertyId
                                    )
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
        props.aspect?.let {
            span(classes = "aspect-edit-console--property-aspect-label") {
                +" : ${it.measure} : ${it.domain} : ${it.baseType}"
            }
            descriptionComponent(
                className = "aspect-edit-console--property-aspect-description",
                description = it.description
            )
        }
    }

    interface Props : RProps {
        var aspect: AspectData?
        var onAspectSelected: (AspectData) -> Unit
        var parentAspectId: String?
        var aspectPropertyId: String?
    }
}

fun RBuilder.aspectPropertyAspect(block: RHandler<AspectPropertyAspectSelector.Props>) =
    child(AspectPropertyAspectSelector::class, block)