package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.wrappers.select.OptionComponentProps
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.asyncSelect
import com.infowings.catalog.wrappers.table.RTableRendererProps
import kotlinext.js.jsObject
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeoutOrNull
import org.w3c.dom.events.Event
import react.*

fun measurementUnitAspectCell(selectOptionHandler: AspectData.(String) -> Unit) = rFunction<RTableRendererProps>("MeasurementUnitSelect") { props ->
    child(MeasurementUnitSuggestingInput::class) {
        attrs {
            measurementUnitName = (props.value as String?) ?: ""
            onOptionSelected = { selectOptionHandler((props.original as AspectRow).aspect, it) }
        }
    }
}

fun measurementUnitAspectPropertyCell(selectOptionHandler: AspectData.(String) -> Unit) = rFunction<RTableRendererProps>("MeasurementUnitSelect") { props ->
    child(MeasurementUnitSuggestingInput::class) {
        attrs {
            measurementUnitName = (props.value as String?) ?: ""
            onOptionSelected = { selectOptionHandler((props.original as AspectPropertyRow).aspect, it) }
        }
    }
}

interface MeasurementUnitOption : SelectOption {
    var measurementUnit: String
}

fun measurementUnitOption(optionName: String) = jsObject<MeasurementUnitOption> {
    measurementUnit = optionName
}

class MeasurementUnitSuggestingInput : RComponent<MeasurementUnitSuggestingInput.Props, RState>() {

    override fun RBuilder.render() {
        asyncSelect<MeasurementUnitOption> {
            attrs {
                className = "aspect-table-select"
                value = props.measurementUnitName
                labelKey = "measurementUnit"
                valueKey = "measurementUnit"
                onChange = { props.onOptionSelected(it.measurementUnit) }
                cache = false
                clearable = false
                options = arrayOf(measurementUnitOption(props.measurementUnitName))
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
                            options = arrayOf(measurementUnitOption(props.measurementUnitName))
                        })
                    }
                    false // Hack to not return Unit from the function that is considered true if placed in `if (Unit)` in javascript
                }
                optionComponent = MeasurementUnitSuggestingOption::class.js as RClass<OptionComponentProps<MeasurementUnitOption>>
            }
        }
    }

    interface Props : RProps {
        var measurementUnitName: String
        var onOptionSelected: (String) -> Unit
    }
}

