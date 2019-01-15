package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.common.BadRequest
import com.infowings.catalog.common.Measure
import com.infowings.catalog.common.MeasureGroup
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.objekt.ValueRecalculationResponse
import com.infowings.catalog.errors.showError
import com.infowings.catalog.objects.edit.tree.utils.InputValidationException
import com.infowings.catalog.objects.edit.tree.utils.isDecimal
import com.infowings.catalog.objects.edit.tree.utils.validate
import com.infowings.catalog.objects.recalculateValue
import com.infowings.catalog.utils.BadRequestException
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

private suspend fun recalcIfDecimal(value: String, from: String, to: String) =
    if (value.isDecimal()) recalculateValue(from, to, value) else ValueRecalculationResponse(from, value)

class ValueMeasureSelectComponent : RComponent<ValueMeasureSelectComponent.Props, ValueMeasureSelectComponent.State>() {

    private fun recalculationConfirmed() {
        val oldMeasureName = props.currentMeasure.name
        val newMeasure = state.nextMeasure
        newMeasure?.let {
            launch {
                val newValueRepr = recalcIfDecimal(props.value.valueRepr, oldMeasureName, it.name)
                val newUpbRepr = recalcIfDecimal(props.value.upbRepr, oldMeasureName, it.name)
                props.onMeasureSelected(it, props.value.copy(valueRepr = newValueRepr.value, upbRepr = newUpbRepr.value))
            }
            setState {
                nextMeasure = null
            }
        }
    }

    private fun recalculationRejected() {
        val newMeasure = state.nextMeasure
        newMeasure?.let {
            props.onMeasureSelected(it, props.value)
            setState {
                nextMeasure = null
            }
        }
    }

    private fun handleNewMeasureSelected(option: ValueMeasureOption) {
        try {
            props.value.validate()
            if (props.currentMeasure.name == option.measure.name) {
                props.onMeasureSelected(option.measure, props.value)
            } else {
                setState {
                    nextMeasure = option.measure
                }
            }
        } catch (e: InputValidationException) {
            showError(BadRequestException(e.message ?: "invalid value", 300.0))
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
        var value: ObjectValueData.DecimalValue
        var currentMeasure: Measure<*>
        var onMeasureSelected: (Measure<*>, ObjectValueData.DecimalValue) -> Unit
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
    value: ObjectValueData.DecimalValue,
    onMeasureSelected: (Measure<*>, ObjectValueData.DecimalValue) -> Unit,
    disabled: Boolean
) = child(ValueMeasureSelectComponent::class) {
    attrs {
        this.measureGroup = measureGroup
        this.currentMeasure = currentMeasure
        this.value = value
        this.onMeasureSelected = onMeasureSelected
        this.disabled = disabled
    }
}