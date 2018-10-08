package com.infowings.catalog.objects.filter

import com.infowings.catalog.common.ObjectGetResponse
import com.infowings.catalog.objects.getSuggestedObjects
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import react.*


private interface ObjectOption : SelectOption {
    var label: String
    var data: ObjectGetResponse
}

private fun objectOption(data: ObjectGetResponse) = jsObject<ObjectOption> {
    this.label = "${data.name} (${data.subjectName})"
    this.data = data
}

class ObjectExcludeFilterComponent : RComponent<ObjectExcludeFilterComponent.Props, RState>() {

    override fun RBuilder.render() {
        asyncSelect<ObjectOption> {
            attrs {
                className = "object-filter-exclude"
                multi = true
                placeholder = "Exclude objects from filtering..."
                value = props.selected.map { objectOption(it) }.toTypedArray()
                labelKey = "label"
                valueKey = "data"
                cache = false
                onChange = {
                    props.onChange(it.unsafeCast<Array<ObjectOption>>().map { it.data }) // TODO: KS-143
                }
                filterOptions = { options, _, _ -> options }
                loadOptions = { input, callback ->
                    if (input.isNotEmpty()) {
                        launch {
                            val suggested = getSuggestedObjects(input)

                            val selectedGuids = props.selected.map { it.guid ?: "" }.toSet()

                            callback(null, jsObject {
                                options = suggested.objects.filterNot { selectedGuids.contains(it.guid) }.map { objectOption(it) }
                                    .toTypedArray() // suggestedAspects.aspects.map { objectOption(it) }.toTypedArray()
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
        var selected: List<ObjectGetResponse>
        var onChange: (List<ObjectGetResponse>) -> Unit
    }
}

fun RBuilder.objectExcludeFilterComponent(block: RHandler<ObjectExcludeFilterComponent.Props>) =
    child(ObjectExcludeFilterComponent::class, block)
