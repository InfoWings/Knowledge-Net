package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.wrappers.blueprint.EditableText
import react.RBuilder

fun RBuilder.propertyName(value: String, onEdit: () -> Unit, onCancel: (String) -> Unit, onChange: (String) -> Unit) =
    EditableText {
        attrs {
            className = "object-property-input-name"
            placeholder = "Enter role name"
            this.value = value
            this.onEdit = onEdit
            this.onCancel = onCancel
            this.onChange = onChange
        }
    }
