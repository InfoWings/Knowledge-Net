package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.wrappers.blueprint.EditableText
import react.RBuilder

fun RBuilder.name(className: String? = null, value: String, onCancel: (String) -> Unit, onChange: (String) -> Unit, disabled: Boolean = false) =
    EditableText {
        attrs {
            this.className = "object-input-name${className?.let { " $it" }}"
            placeholder = "Enter name"
            this.value = value
            this.onCancel = onCancel
            this.onChange = onChange
            this.disabled = disabled
        }
    }
