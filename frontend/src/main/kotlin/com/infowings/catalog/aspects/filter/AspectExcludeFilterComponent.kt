package com.infowings.catalog.aspects.filter

import com.infowings.catalog.aspects.getSuggestedAspects
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import react.*

private interface AspectOption : SelectOption {
    var aspectLabel: String
    var aspectData: AspectData
}

private fun aspectOption(aspectData: AspectData) = jsObject<AspectOption> {
    this.aspectLabel =
            "${aspectData.name ?: error("aspectData should have name")} (${aspectData.subject?.name ?: "Global"})"
    this.aspectData = aspectData
}

class AspectExcludeFilterComponent : RComponent<AspectExcludeFilterComponent.Props, RState>() {

    override fun RBuilder.render() {
        asyncSelect<AspectOption> {
            attrs {
                className = "aspect-filter-exclude"
                multi = true
                placeholder = "Exclude aspects from filtering..."
                value = props.selectedAspects.map { aspectOption(it) }.toTypedArray()
                labelKey = "aspectLabel"
                valueKey = "aspectLabel"
                onChange = {
                    props.onChange(it.unsafeCast<Array<AspectOption>>().map { it.aspectData }) // TODO: KS-143
                }
                loadOptions = { input, callback ->
                    if (input.isNotEmpty()) {
                        launch {
                            val suggestedAspects = getSuggestedAspects(input, null, null)
                            callback(null, jsObject {
                                options = suggestedAspects.aspects.map { aspectOption(it) }.toTypedArray()
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
        var selectedAspects: List<AspectData>
        var onChange: (List<AspectData>) -> Unit
    }
}

fun RBuilder.aspectExcludeFilterComponent(block: RHandler<AspectExcludeFilterComponent.Props>) =
    child(AspectExcludeFilterComponent::class, block)
