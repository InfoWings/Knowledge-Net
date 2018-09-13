package com.infowings.catalog.objects.view.tree.format

import com.infowings.catalog.common.LinkValueData
import com.infowings.catalog.common.ObjectValueData
import react.RBuilder
import react.dom.span

fun RBuilder.valueFormat(value: ObjectValueData) {
    when (value) {
        is ObjectValueData.NullValue -> return
        is ObjectValueData.DecimalValue -> span(classes = "object-property-value-line__value") { +value.valueRepr }
        is ObjectValueData.StringValue -> span(classes = "object-property-value-line__value") { +value.value }
        is ObjectValueData.BooleanValue -> booleanValueFormat(value.value)
        is ObjectValueData.IntegerValue -> span(classes = "object-property-value-line__value") { +value.value.toString() }
        is ObjectValueData.Link -> when (value.value) {
            is LinkValueData.DomainElement -> domainElementReferenceFormat(value.value.id)
            else -> referenceBaseTypeFormat(value.value)
        }
    }
}

fun RBuilder.booleanValueFormat(booleanValue: Boolean) {
    val classNameModifier = booleanValue.toString()
    span(classes = "object-property-value-line__value object-value__boolean--$classNameModifier") {
        booleanValue.toString()
    }
}
