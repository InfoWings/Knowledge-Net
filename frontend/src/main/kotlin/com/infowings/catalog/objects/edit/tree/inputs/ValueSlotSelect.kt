package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.objects.edit.tree.ValueSlot
import com.infowings.catalog.wrappers.blueprint.Popover
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.commonSelect
import kotlinext.js.jsObject
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState

interface ValueSlotOption : SelectOption {
    var label: String
    var slot: ValueSlot
}

private fun valueSlotOption(slot: ValueSlot): ValueSlotOption = jsObject {
    this.label = if (slot == ValueSlot.New) "<NEW>" else "${slot.pos}:${slot.oPropertyName ?: "<Unnamed>"}"
    this.slot = slot
}

class ValueSlotSelectComponent : RComponent<ValueSlotSelectComponent.Props, ValueSlotSelectComponent.State>() {
    private fun handleSlotSelected(option: ValueSlotOption) {
        props.onSlotSelected(option.slot)
    }

    override fun RBuilder.render() {
        val allOptions = props.slots.map(::valueSlotOption).toTypedArray()
        Popover {
            commonSelect<ValueSlotOption> {
                attrs {
                    className = "value-measure-select"
                    value = props.selectedSlot ?.let { valueSlotOption(it) }
                    labelKey = "label"
                    valueKey = "label"
                    onChange = ::handleSlotSelected
                    options = allOptions
                    clearable = false
                }
            }
        }
    }

    interface Props : RProps {
        var slots: List<ValueSlot>
        var onSlotSelected: (ValueSlot) -> Unit
        var selectedSlot: ValueSlot?
    }

    interface State : RState {
    }
}

fun RBuilder.valueSlotSelect(
    slots: List<ValueSlot>,
    onSlotSelected: (ValueSlot) -> Unit,
    selectedSlot: ValueSlot?
) = child(ValueSlotSelectComponent::class) {
    attrs {
        this.slots = slots
        this.onSlotSelected = onSlotSelected
        this.selectedSlot = selectedSlot
    }
}