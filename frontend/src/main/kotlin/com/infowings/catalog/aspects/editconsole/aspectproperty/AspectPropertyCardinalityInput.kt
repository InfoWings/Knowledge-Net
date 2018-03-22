package com.infowings.catalog.aspects.editconsole.aspectproperty

import com.infowings.catalog.wrappers.react.label
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.commonSelect
import kotlinext.js.jsObject
import react.*
import react.dom.div

private interface CardinalityOption : SelectOption {
    var cardinality: String
}

private fun cardinalityOption(cardinality: String) = jsObject<CardinalityOption> {
    this.cardinality = cardinality
}

class AspectPropertyCardinalityInput : RComponent<AspectPropertyCardinalityInput.Props, RState>() {

    private fun handleSelectCardinalityOption(option: CardinalityOption) {
        props.onChange(option.cardinality)
    }

    override fun RBuilder.render() {
        div(classes = "aspect-edit-console--aspect-property-input-container") {
            label(classes = "aspect-edit-console--input-label", htmlFor = "aspect-property-cardinality") {
                +"Cardinality"
            }
            div(classes = "aspect-edit-console--input-wrapper") {
                commonSelect<CardinalityOption> {
                    attrs {
                        className = "aspect-table-select"
                        value = props.value ?: ""
                        labelKey = "cardinality"
                        valueKey = "cardinality"
                        onChange = ::handleSelectCardinalityOption
                        clearable = false
                        options = arrayOf(
                                cardinalityOption("ZERO"),
                                cardinalityOption("ONE"),
                                cardinalityOption("INFINITY")
                        )
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

fun RBuilder.aspectPropertyCardinality(block: RHandler<AspectPropertyCardinalityInput.Props>) = child(AspectPropertyCardinalityInput::class, block)