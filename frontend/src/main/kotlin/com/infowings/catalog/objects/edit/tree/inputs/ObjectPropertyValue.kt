package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.LinkValueData
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.objects.edit.tree.inputs.values.*
import react.RBuilder

fun RBuilder.propertyValue(
    baseType: BaseType,
    referenceBookId: String?,
    value: ObjectValueData?,
    onChange: (ObjectValueData) -> Unit,
    disabled: Boolean = false
) {
    when {
        baseType == BaseType.Text && referenceBookId != null -> refBookInput(
            if (value is ObjectValueData.Link && value.value is LinkValueData.DomainElement) value.value.id else null,
            { onChange(ObjectValueData.Link(LinkValueData.DomainElement(it))) },
            referenceBookId,
            disabled
        )
        baseType == BaseType.Text -> textInput((value as? ObjectValueData.StringValue)?.asStringValue, disabled) { onChange(ObjectValueData.StringValue(it)) }
        baseType == BaseType.Integer -> integerInput((value as? ObjectValueData.IntegerValue)?.asStringValue, disabled) {
            onChange(
                ObjectValueData.IntegerValue(
                    it.toInt(),
                    null
                )
            )
        }
        baseType == BaseType.Decimal -> decimalInput((value as? ObjectValueData.DecimalValue)?.asStringValue, disabled) {
            onChange(
                ObjectValueData.DecimalValue(
                    it
                )
            )
        }
        baseType == BaseType.Boolean -> booleanInput((value as? ObjectValueData.BooleanValue)?.asStringValue, disabled) {
            onChange(
                ObjectValueData.BooleanValue(
                    it.toBoolean()
                )
            )
        }
    }
}

private val ObjectValueData.asStringValue
    get() = when(this) {
        is ObjectValueData.StringValue -> this.value
        is ObjectValueData.BooleanValue -> this.value.toString()
        is ObjectValueData.IntegerValue -> this.value.toString()
        is ObjectValueData.DecimalValue -> this.valueRepr
        is ObjectValueData.Link -> when(this.value) {
            is LinkValueData.DomainElement -> this.value.id
            else -> TODO("Subject and Object are not implemented")
        }
        else -> throw IllegalArgumentException("$this is not representable by string")
    }

