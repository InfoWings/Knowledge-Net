package com.infowings.catalog.aspects.editconsole.aspectproperty

import com.infowings.catalog.common.AspectPropertyCardinality
import com.infowings.catalog.wrappers.react.label
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.commonSelect
import kotlinext.js.jsObject
import react.*
import react.dom.div

private interface CardinalityOption : SelectOption {
    var cardinalityLabel: String
    var cardinalityEnumValue: String
}

private fun cardinalityOption(cardinality: AspectPropertyCardinality) = jsObject<CardinalityOption> {
    this.cardinalityLabel = cardinality.label
    this.cardinalityEnumValue = cardinality.name
}

class AspectPropertyCardinalityInput : RComponent<AspectPropertyCardinalityInput.Props, RState>() {

    private fun handleSelectCardinalityOption(option: CardinalityOption) {
        props.onChange(option.cardinalityEnumValue)
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
                        labelKey = "cardinalityLabel"
                        valueKey = "cardinalityEnumValue"
                        onChange = ::handleSelectCardinalityOption
                        clearable = false
                        options = AspectPropertyCardinality.values().map { cardinalityOption(it) }.toTypedArray()
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