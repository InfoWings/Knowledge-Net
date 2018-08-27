package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.common.Measure
import com.infowings.catalog.common.MeasureGroup
import com.infowings.catalog.objects.recalculateValue
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Popover
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.commonSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import react.*
import react.dom.div
import react.dom.h5

interface ValueMeasureOption : SelectOption {
    var label: String
    var measure: Measure<*>
}

private fun valueMeasureOption(measure: Measure<*>): ValueMeasureOption = jsObject {
    this.label = measure.symbol
    this.measure = measure
}

class ValueMeasureSelectComponent : RComponent<ValueMeasureSelectComponent.Props, ValueMeasureSelectComponent.State>() {

    private fun recalculationConfirmed() {
        val oldMeasureName = props.currentMeasure.name
        val newMeasure = state.nextMeasure
        newMeasure?.let {
            launch {
                val newValueRepresentation = recalculateValue(oldMeasureName, it.name, props.stringValueRepresentation)
                props.onMeasureSelected(it, newValueRepresentation.value)
            }
            setState {
                nextMeasure = null
            }
        }
    }

    private fun recalculationRejected() {
        val newMeasure = state.nextMeasure
        newMeasure?.let {
            props.onMeasureSelected(it, props.stringValueRepresentation)
            setState {
                nextMeasure = null
            }
        }
    }

    private fun handleNewMeasureSelected(option: ValueMeasureOption) {
        if (props.currentMeasure.name == option.measure.name) {
            props.onMeasureSelected(option.measure, props.stringValueRepresentation)
        } else {
            setState {
                nextMeasure = option.measure
            }
        }
    }

    override fun RBuilder.render() {
        val allOptions = props.measureGroup.measureList.map(::valueMeasureOption).toTypedArray()
        Popover {
            attrs {
                isOpen = state.nextMeasure != null
                onInteraction = { nextOpenState ->
                    if (nextOpenState == false) {
                        setState {
                            nextMeasure = null
                        }
                    }
                }
                content = buildElement { suggestRecalculationPopoverWindow(::recalculationConfirmed, ::recalculationRejected) }
            }
            commonSelect<ValueMeasureOption> {
                attrs {
                    className = "value-measure-select"
                    value = valueMeasureOption(props.currentMeasure)
                    labelKey = "label"
                    valueKey = "label"
                    onChange = ::handleNewMeasureSelected
                    options = allOptions
                    disabled = props.disabled || state.nextMeasure != null
                    clearable = false
                }
            }
        }
    }

    interface Props : RProps {
        var measureGroup: MeasureGroup<*>
        var stringValueRepresentation: String
        var currentMeasure: Measure<*>
        var onMeasureSelected: (Measure<*>, String) -> Unit
        var disabled: Boolean
    }

    interface State : RState {
        var nextMeasure: Measure<*>?
    }
}

fun RBuilder.suggestRecalculationPopoverWindow(
    onConfirmRecalculation: () -> Unit,
    onRejectRecalculation: () -> Unit
) = div(classes = "recalculate-window") {
    div(classes = "recalculate-window__header") {
        h5 {
            +"Recalculate value?"
        }
    }
    div(classes = "recalculate-window__buttons") {
        Button {
            attrs {
                className = "pt-small pt-intent-success"
                onClick = { onConfirmRecalculation() }
            }
            +"Recalculate"
        }
        Button {
            attrs {
                className = "pt-small"
                onClick = { onRejectRecalculation() }
            }
            +"No, I'll do it myself"
        }
    }
}

fun RBuilder.valueMeasureSelect(
    measureGroup: MeasureGroup<*>,
    currentMeasure: Measure<*>,
    stringValueRepresentation: String,
    onMeasureSelected: (Measure<*>, String) -> Unit,
    disabled: Boolean
) = child(ValueMeasureSelectComponent::class) {
    attrs {
        this.measureGroup = measureGroup
        this.currentMeasure = currentMeasure
        this.stringValueRepresentation = stringValueRepresentation
        this.onMeasureSelected = onMeasureSelected
        this.disabled = disabled
    }
}