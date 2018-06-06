package com.infowings.catalog.objects.treeview.inputs

import com.infowings.catalog.wrappers.blueprint.EditableText
import react.RBuilder

fun RBuilder.propertyValue(value: String, onEdit: () -> Unit, onCancel: (String) -> Unit, onChange: (String) -> Unit) =
    EditableText {
        attrs {
            className = "object-property-input-value"
            placeholder = "Enter value"
            this.value = value
            this.onEdit = onEdit
            this.onCancel = onCancel
            this.onChange = onChange
        }
    }
