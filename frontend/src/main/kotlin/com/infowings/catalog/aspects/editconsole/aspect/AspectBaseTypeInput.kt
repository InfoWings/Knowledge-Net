package com.infowings.catalog.aspects.editconsole.aspect

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.wrappers.react.label
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.commonSelect
import kotlinext.js.jsObject
import react.*
import react.dom.div

private interface Option : SelectOption {
    var aspectBaseType: String
}

private fun baseTypeOption(baseType: String) = jsObject<Option> {
    aspectBaseType = baseType
}

class AspectBaseTypeInput : RComponent<AspectBaseTypeInput.Props, RState>() {

    private fun handleSelectBaseTypeOption(option: Option) {
        props.onChange(option.aspectBaseType)
    }

    override fun RBuilder.render() {
        div(classes = "aspect-edit-console--aspect-input-container") {
            label(classes = "aspect-edit-console--input-label", htmlFor = "aspect-base-type") {
                +"Base Type"
            }
            div(classes = "aspect-edit-console--input-wrapper") {
                commonSelect<Option> {
                    attrs {
                        className = "aspect-table-select"
                        value = props.value ?: ""
                        labelKey = "aspectBaseType"
                        valueKey = "aspectBaseType"
                        onChange = ::handleSelectBaseTypeOption
                        clearable = false
                        disabled = !props.measureUnit.isNullOrEmpty()
                        options = arrayOf(
                                baseTypeOption(BaseType.Binary.name),
                                baseTypeOption(BaseType.Boolean.name),
                                baseTypeOption(BaseType.Decimal.name),
                                baseTypeOption(BaseType.Integer.name),
                                baseTypeOption(BaseType.Long.name),
                                baseTypeOption(BaseType.Nothing.name),
                                baseTypeOption(BaseType.Text.name)
                        )
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var measureUnit: String?
        var value: String?
        var onChange: (String) -> Unit
    }

}

fun RBuilder.aspectBaseTypeInput(block: RHandler<AspectBaseTypeInput.Props>) = child(AspectBaseTypeInput::class, block)