package com.infowings.catalog.aspects.editconsole.aspect

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.wrappers.react.label
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.commonSelect
import kotlinext.js.jsObject
import react.*
import react.dom.div

private interface BaseTypeOption : SelectOption {
    var aspectBaseType: String
}

private fun baseTypeOption(baseType: String) = jsObject<BaseTypeOption> {
    aspectBaseType = baseType
}

class AspectBaseTypeInput : RComponent<AspectBaseTypeInput.Props, RState>() {

    private fun handleSelectBaseTypeOption(option: BaseTypeOption?) {
        option?.let { props.onChange(it.aspectBaseType) } ?: props.onChange(null)
    }

    override fun RBuilder.render() {
        div(classes = "aspect-edit-console--aspect-input-container") {
            label(classes = "aspect-edit-console--input-label", htmlFor = "aspect-base-type") {
                +"Base Type"
            }
            div(classes = "aspect-edit-console--input-wrapper") {
                commonSelect<BaseTypeOption> {
                    attrs {
                        className = "aspect-table-select"
                        value = props.value ?: ""
                        labelKey = "aspectBaseType"
                        valueKey = "aspectBaseType"
                        onChange = ::handleSelectBaseTypeOption
                        clearable = false
                        resetValue = null
                        disabled = props.disabled
                        options = arrayOf(
                            baseTypeOption(BaseType.Boolean.name),
                            baseTypeOption(BaseType.Decimal.name),
                            baseTypeOption(BaseType.Integer.name),
                            baseTypeOption(BaseType.Text.name),
                            baseTypeOption(BaseType.Reference.name)
                        )
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var value: String?
        var disabled: Boolean
        var onChange: (String?) -> Unit
    }

}

fun RBuilder.aspectBaseTypeInput(block: RHandler<AspectBaseTypeInput.Props>) = child(AspectBaseTypeInput::class, block)