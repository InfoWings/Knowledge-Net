package com.infowings.catalog.aspects.editconsole.aspectproperty

import com.infowings.catalog.aspects.getAspectHints
import com.infowings.catalog.aspects.listEntry
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectHint
import com.infowings.catalog.common.AspectHintSource
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import com.infowings.catalog.wrappers.react.asReactElement
import com.infowings.catalog.wrappers.react.label
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import react.*
import react.dom.div
import react.dom.span

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


class AspectPropertyAspectSelector : RComponent<AspectPropertyAspectSelector.Props, RState>(), JobCoroutineScope by JobSimpleCoroutineScope() {
    /*
        private fun handleSelectAspectOption(option: AspectOption) {
            props.onAspectSelected(option.aspectData)
        }
    */
    override fun componentWillMount() {
        job = Job()
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun RBuilder.render() {
        val boundAspect = props.aspect

        println("bound: " + boundAspect)

        val selected = boundAspect?.let { aspectOptionSelected(AspectHint.byAspect(it, AspectHintSource.ASPECT_NAME)) }
        val selectedArray = selected?.let { arrayOf(it) } ?: emptyArray()

        div(classes = "aspect-edit-console--aspect-property-input-container") {
            label(classes = "aspect-edit-console--input-label") {
                +"Aspect"
            }
            div(classes = "aspect-edit-console--input-wrapper") {
                asyncSelect<AspectOption> {
                    attrs {
                        className = "aspect-table-select"
                        value = selected
                        labelKey = "aspectEntry"
                        onChange = {
                            props.onAspectSelected(it.aspectHint) // TODO: KS-143
                        }
                        cache = false
                        clearable = false
                        options = selectedArray
                        filterOptions = { options, _, _ -> options }
                        loadOptions = { input, callback ->
                            if (input.isNotEmpty()) {
                                launch {
                                    val hints = getAspectHints(input, props.parentAspectId, props.aspectPropertyId).defaultOrder()
                                    println("hinted aspects: " + hints.map { it.name })
                                    callback(null, jsObject {
                                        options = hints.map { aspectOption(it) }.toTypedArray()
                                    })
                                }
                            } else {
                                callback(null, jsObject {
                                    options = selectedArray
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
                +" : ${it.measure ?: ""} : ${it.domain ?: ""} : ${it.baseType ?: ""}"
            }
            descriptionComponent(
                className = "aspect-edit-console--property-aspect-description",
                description = it.description
            )
        }
    }

    interface Props : RProps {
        var aspect: AspectData?
        var onAspectSelected: (AspectHint) -> Unit
        var parentAspectId: String?
        var aspectPropertyId: String?
    }
}

fun RBuilder.aspectPropertyAspect(block: RHandler<AspectPropertyAspectSelector.Props>) =
    child(AspectPropertyAspectSelector::class, block)