package com.infowings.catalog.objects.view.tree.format

import com.infowings.catalog.common.LinkValueData
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.RangeFlagConstants
import com.infowings.catalog.wrappers.History
import react.RBuilder
import react.dom.span

fun RBuilder.valueFormat(value: ObjectValueData, history: History) {
    when (value) {
        is ObjectValueData.NullValue -> return
        is ObjectValueData.DecimalValue -> span(classes = "object-property-value-line__value") {
            if (value.upbRepr != value.valueRepr) {
                val leftInf = RangeFlagConstants.LEFT_INF.bitmask.and(value.rangeFlags) != 0
                val rightInf = RangeFlagConstants.RIGHT_INF.bitmask.and(value.rangeFlags) != 0
                val leftText = if (leftInf) "-Inf" else value.valueRepr
                val rightText = if (rightInf) "Inf" else value.upbRepr
                +"[$leftText‥$rightText]"
            } else {
                +value.valueRepr
            }
        }
        is ObjectValueData.StringValue -> span(classes = "object-property-value-line__value") { +value.value }
        is ObjectValueData.BooleanValue -> booleanValueFormat(value.value)
        is ObjectValueData.IntegerValue -> span(classes = "object-property-value-line__value") {
            +value.value.toString()
            if (value.upb != value.value) {
                +(" : " + value.upb)
            }
        }
        is ObjectValueData.Link -> when (value.value) {
            is LinkValueData.DomainElement -> domainElementReferenceFormat(value.value.id)
            else -> referenceBaseTypeFormat(value.value, history)
        }
    }
}

fun RBuilder.booleanValueFormat(booleanValue: Boolean) {
    val classNameModifier = booleanValue.toString()
    span(classes = "object-property-value-line__value object-value__boolean--$classNameModifier") {
        +(if (booleanValue) "ON" else "OFF")
    }
}
