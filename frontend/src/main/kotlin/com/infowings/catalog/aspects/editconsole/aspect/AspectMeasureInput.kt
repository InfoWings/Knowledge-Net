package com.infowings.catalog.aspects.editconsole.aspect

import com.infowings.catalog.aspects.MeasurementUnitOption
import com.infowings.catalog.aspects.MeasurementUnitSuggestingOption
import com.infowings.catalog.aspects.getSuggestedMeasurementUnits
import com.infowings.catalog.wrappers.react.label
import com.infowings.catalog.wrappers.select.OptionComponentProps
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeoutOrNull
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.*
import react.dom.div

interface MeasurementUnitOption : SelectOption {
    var measurementUnit: String
}

private fun measurementUnitOption(optionName: String) = jsObject<MeasurementUnitOption> {
    measurementUnit = optionName
}

class AspectMeasureInput : RComponent<AspectMeasureInput.Props, RState>() {

    private fun handleInputFieldChanged(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        props.onChange(e.target.unsafeCast<HTMLInputElement>().value)
    }

    override fun RBuilder.render() {
        div(classes = "aspect-edit-console--aspect-input-container") {
            label(classes = "aspect-edit-console--input-label") {
                +"Measure"
            }
            div(classes = "aspect-edit-console--input-wrapper") {
                asyncSelect<MeasurementUnitOption> {
                    attrs {
                        className = "aspect-table-select"
                        value = props.value ?: ""
                        labelKey = "measurementUnit"
                        valueKey = "measurementUnit"
                        onChange = { props.onChange(it.measurementUnit) }
                        cache = false
                        onSelectResetsInput = false
                        clearable = true
                        resetValue = ""
                        options = if (props.value.isNullOrEmpty()) emptyArray()
                        else arrayOf(measurementUnitOption(props.value!!))
                        loadOptions = { input, callback ->
                            if (input.isNotEmpty()) {
                                launch {
                                    val measurementUnitsArray = withTimeoutOrNull(500) {
                                        getSuggestedMeasurementUnits(input)
                                    }
                                    callback(null, jsObject {
                                        options = measurementUnitsArray?.map { measurementUnitOption(it) }?.toTypedArray() ?: emptyArray()
                                    })
                                }
                            } else {
                                callback(null, jsObject {
                                    options = if (props.value.isNullOrEmpty()) emptyArray()
                                    else arrayOf(measurementUnitOption(props.value!!), measurementUnitOption(""))
                                })
                            }
                            false // Hack to not return Unit from the function that is considered true if placed in `if (Unit)` in javascript
                        }
                        optionComponent = MeasurementUnitSuggestingOption::class.js.unsafeCast<RClass<OptionComponentProps<MeasurementUnitOption>>>()
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var value: String?
        var onChange: (String) -> Unit
    }

}

fun RBuilder.aspectMeasureInput(block: RHandler<AspectMeasureInput.Props>) = child(AspectMeasureInput::class, block)