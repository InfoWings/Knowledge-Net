package com.infowings.catalog.objects.treeview.inputs

import com.infowings.catalog.common.BaseType
import com.infowings.catalog.objects.treeview.inputs.values.booleanInput
import com.infowings.catalog.objects.treeview.inputs.values.decimalInput
import com.infowings.catalog.objects.treeview.inputs.values.integerInput
import com.infowings.catalog.objects.treeview.inputs.values.textInput
import react.RBuilder

fun RBuilder.propertyValue(value: String?, baseType: String, onEdit: () -> Unit, onChange: (String) -> Unit) {
    when (baseType) {
        BaseType.Text.name -> textInput(value, onChange, onEdit)
        BaseType.Integer.name -> integerInput(value, onChange)
        BaseType.Decimal.name -> decimalInput(value, onChange)
        BaseType.Boolean.name -> booleanInput(value, onChange)
    }
}
