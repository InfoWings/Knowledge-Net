package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.common.*
import com.infowings.catalog.objects.edit.tree.inputs.values.*
import com.infowings.catalog.reference.book.getReferenceBookItem
import kotlinx.atomicfu.AtomicRef
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import react.RBuilder
import react.ReactElement

fun RBuilder.propertyValue(
    baseType: BaseType,
    referenceBookId: String?,
    referenceBookNameSoft: String?,
    value: ObjectValueData?,
    onChange: (ObjectValueData) -> Unit,
    disabled: Boolean = false
): ReactElement? {
    println("BT: ${baseType.name}, rbi: refBookId: $referenceBookId, rbi: refBookNameSoft: $referenceBookNameSoft, value: $value, disabled: $disabled")

    val domainElement = if (value is ObjectValueData.Link && value.value is LinkValueData.DomainElement) {
        value.value as? LinkValueData.DomainElement
    } else null

    return when {
        baseType == BaseType.Text && referenceBookId != null && (referenceBookId == domainElement?.rootId || value == null ) -> {
            refBookInput(
                domainElement?.id,
                { onChange(ObjectValueData.Link(LinkValueData.DomainElement(it, "", referenceBookId))) },
                referenceBookId,
                disabled
            )
        }
        baseType == BaseType.Text -> {
            val textValue = if (domainElement != null) domainElement.value else (value as? ObjectValueData.StringValue)?.asStringValue
            textInput(textValue, disabled) {
                onChange(ObjectValueData.StringValue(it))
            }
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
            rangedDecimalInput(ObjectValueData.DecimalValue(decValue.valueRepr, decValue.upbRepr, decValue.rangeFlags), { lwb, upb, rangeFlags ->
                val leftFlag = RangeFlagConstants.LEFT_INF.bitmask
                val rightFlag = RangeFlagConstants.RIGHT_INF.bitmask

                val leftInfinity = rangeFlags.and(leftFlag) != 0
                val rightInfinity = rangeFlags.and(rightFlag) != 0

                onChange(ObjectValueData.DecimalValue(if (leftInfinity) "0" else lwb, if (rightInfinity) "0" else upb, rangeFlags))
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
