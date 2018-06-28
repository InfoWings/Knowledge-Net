package com.infowings.catalog.objects.treeedit.inputs

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.objects.treeedit.inputs.values.*
import react.RBuilder

fun RBuilder.propertyValue(
    value: String?,
    baseType: String,
    onEdit: () -> Unit,
    onChange: (String) -> Unit,
    aspectRefBookId: String?
) {
    when {
        baseType == BaseType.Text.name && aspectRefBookId != null -> refBookInput(value, onChange, aspectRefBookId)
        baseType == BaseType.Text.name -> textInput(value, onChange, onEdit)
        baseType == BaseType.Integer.name -> integerInput(value, onChange)
        baseType == BaseType.Decimal.name -> decimalInput(value, onChange)
        baseType == BaseType.Boolean.name -> booleanInput(value, onChange)
    }
}
