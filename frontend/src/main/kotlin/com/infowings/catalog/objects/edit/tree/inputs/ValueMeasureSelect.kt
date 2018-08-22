package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.common.Measure
import com.infowings.catalog.common.MeasureGroup
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.commonSelect
import kotlinext.js.jsObject
import react.RBuilder
import react.RProps
import react.rFunction

interface ValueMeasureOption : SelectOption {
    var label: String
    var measure: Measure<*>?
}

private fun valueMeasureOption(measure: Measure<*>): ValueMeasureOption = jsObject {
    this.label = measure.symbol
    this.measure = measure
}

val valueMeasureSelectComponent = rFunction<ValueMeasureSelectComponentProps>("ValueMeasureSelectComponent") { props ->
    val allOptions = props.measureGroup.measureList.map(::valueMeasureOption).toTypedArray()
    commonSelect<ValueMeasureOption> {
        attrs {
            className = "value-measure-select"
            value = valueMeasureOption(props.currentMeasure)
            labelKey = "label"
            valueKey = "label"
            onChange = { props.onMeasureSelected(it.measure) }
            options = allOptions
            disabled = props.disabled
            clearable = false
        }
    }
}

interface ValueMeasureSelectComponentProps : RProps {
    var measureGroup: MeasureGroup<*>
    var currentMeasure: Measure<*>
    var onMeasureSelected: (Measure<*>?) -> Unit
    var disabled: Boolean
}

fun RBuilder.valueMeasureSelect(
    measureGroup: MeasureGroup<*>,
    currentMeasure: Measure<*>,
    onMeasureSelected: (Measure<*>?) -> Unit,
    disabled: Boolean
) = valueMeasureSelectComponent {
    attrs {
        this.measureGroup = measureGroup
        this.currentMeasure = currentMeasure
        this.onMeasureSelected = onMeasureSelected
        this.disabled = disabled
    }
}