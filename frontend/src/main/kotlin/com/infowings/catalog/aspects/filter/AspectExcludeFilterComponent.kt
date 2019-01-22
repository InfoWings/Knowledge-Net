package com.infowings.catalog.aspects.filter

import com.infowings.catalog.aspects.getAspectHints
import com.infowings.catalog.aspects.listEntry
import com.infowings.catalog.common.AspectHint
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import com.infowings.catalog.wrappers.react.asReactElement
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import react.*

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

class AspectExcludeFilterComponent : RComponent<AspectExcludeFilterComponent.Props, RState>(), JobCoroutineScope by JobSimpleCoroutineScope() {
    override fun componentWillMount() {
        job = Job()
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun RBuilder.render() {
        asyncSelect<AspectOption> {
            attrs {
                className = "aspect-filter-exclude"
                multi = true
                placeholder = "Exclude aspects from filtering..."
                value = props.selectedAspects.map { aspectOptionSelected(it) }.toTypedArray()
                labelKey = "aspectEntry"
                valueKey = "aspectEntry"
                cache = false
                onChange = {
                    println("A hints: ${it.unsafeCast<Array<AspectOption>>().toList().map { it.aspectHint }}")
                    props.onChange(it.unsafeCast<Array<AspectOption>>().map { it.aspectHint }) // TODO: KS-143
                }
                filterOptions = { options, _, _ -> options }
                loadOptions = { input, callback ->
                    if (input.length > 2) {
                        launch {
                            //val suggestedAspects = getSuggestedAspects(input, null, null)
                            val hints = getAspectHints(input)
                            println("loaded hint names: ${hints.byAspectName.map { it.aspectName}}")
                            callback(null, jsObject {
                                options = hints.defaultOrder().map { aspectOption(it) }.toTypedArray()
                            })
                        }
                    } else {
                        callback(null, jsObject {
                            options = emptyArray()
                        })
                    }
                    false
                }
                clearable = false
            }
        }
    }

    interface Props : RProps {
        var selectedAspects: List<AspectHint>
        var onChange: (List<AspectHint>) -> Unit
    }
}

fun RBuilder.aspectExcludeFilterComponent(block: RHandler<AspectExcludeFilterComponent.Props>) =
    child(AspectExcludeFilterComponent::class, block)
