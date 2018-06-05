package com.infowings.catalog.objects.treeview.inputs

import com.infowings.catalog.wrappers.blueprint.EditableText
import react.RBuilder

fun RBuilder.name(value: String, onEdit: () -> Unit, onCancel: (String) -> Unit, onChange: (String) -> Unit) =
    EditableText {
        attrs {
            className = "object-input-name"
            placeholder = "Enter name"
            this.value = value
            this.onEdit = onEdit
            this.onCancel = onCancel
            this.onChange = onChange
        }
    }
