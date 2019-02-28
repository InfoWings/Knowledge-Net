package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.LinkValueData
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.RangeFlagConstants
import com.infowings.catalog.objects.edit.tree.inputs.values.*
import com.infowings.catalog.objects.edit.tree.utils.transform
import react.RBuilder
import react.ReactElement

fun RBuilder.propertyValue(
    baseType: BaseType,
    referenceBookId: String?,
    value: ObjectValueData?,
    onChange: (ObjectValueData) -> Unit,
    disabled: Boolean = false,
    onSubmit: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
): ReactElement? {
    val stringValue = value as? ObjectValueData.StringValue

    val domainElement = if (value is ObjectValueData.Link && value.value is LinkValueData.DomainElement) {
        value.value
    } else null

    return when {
        baseType == BaseType.Text && referenceBookId != null &&
                (referenceBookId == domainElement?.rootId || value == null || stringValue?.value == "") ->
            refBookInput(
                domainElement?.id,
                { onChange(ObjectValueData.Link(LinkValueData.DomainElement(it, "", "", referenceBookId))) },
                referenceBookId,
                disabled
            )

        baseType == BaseType.Text -> {
            val textValue = domainElement?.value ?: stringValue?.asStringValue
            textInput(textValue, disabled, { onChange(ObjectValueData.StringValue(it)) }, onSubmit, onCancel)
        }

        baseType == BaseType.Reference -> {
            entityLinkInput((value as? ObjectValueData.Link)?.value, { it?.let { onChange(ObjectValueData.Link(it)) } }, disabled)
        }
        baseType == BaseType.Integer -> {
            rangedNumericInput(value as ObjectValueData.IntegerValue, { lwb, upb ->
                onChange(ObjectValueData.IntegerValue(lwb, upb, null))
            }, false)
        }

        baseType == BaseType.Decimal -> {
            val decValue = value as ObjectValueData.DecimalValue
            rangedDecimalInput(ObjectValueData.DecimalValue(decValue.valueRepr, decValue.upbRepr, decValue.rangeFlags), { lwb, upb, isRange ->
                val leftFlag = RangeFlagConstants.LEFT_INF.flag(lwb == "")
                val rightFlag = RangeFlagConstants.RIGHT_INF.flag(upb == "")
                val rangeFlag = RangeFlagConstants.RANGE.flag(isRange)

                val rangeFlags = leftFlag.or(rightFlag).or(rangeFlag)

                onChange(ObjectValueData.DecimalValue(lwb, if (isRange) upb else lwb, rangeFlags).transform())
            }, disabled)
        }

        baseType == BaseType.Boolean -> booleanInput((value as? ObjectValueData.BooleanValue)?.asStringValue, disabled) {
            onChange(
                ObjectValueData.BooleanValue(
                    it.toBoolean()
                )
            )
        }

        else -> null
    }
}


@Suppress("NotImplementedDeclaration")
private val ObjectValueData.asStringValue
    get() = when (this) {
        is ObjectValueData.NullValue -> null
        is ObjectValueData.StringValue -> this.value
        is ObjectValueData.BooleanValue -> this.value.toString()
        is ObjectValueData.IntegerValue -> this.value.toString()
        is ObjectValueData.DecimalValue -> this.valueRepr
        is ObjectValueData.Link -> when (this.value) {
            is LinkValueData.DomainElement -> this.value.id
            is LinkValueData.Object -> this.value.id
            is LinkValueData.ObjectValue -> this.value.id
            else -> TODO("Other value link types are not yet implemented")
        }
        else -> throw IllegalArgumentException("$this is not representable by string")
    }
