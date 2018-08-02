package com.infowings.catalog.objects.edit.tree.inputs.values

import com.infowings.catalog.objects.edit.tree.inputs.referenceBookInput
import com.infowings.catalog.wrappers.blueprint.EditableText
import com.infowings.catalog.wrappers.blueprint.NumericInput
import com.infowings.catalog.wrappers.blueprint.Switch
import org.w3c.dom.HTMLInputElement
import react.RBuilder

fun RBuilder.textInput(value: String?, disabled: Boolean, onUpdate: (String) -> Unit) = EditableText {
    attrs {
        this.value = value ?: ""
        placeholder = "Enter property value"
        onCancel = onUpdate
        onChange = onUpdate
        this.disabled = disabled
    }
}

fun RBuilder.integerInput(value: String?, disabled: Boolean, onUpdate: (String) -> Unit) = NumericInput {
    attrs {
        this.value = value ?: "0"
        this.onValueChange = { valueAsNumber, _ -> onUpdate(valueAsNumber.toInt().toString()) }
        majorStepSize = 1
        minorStepSize = 1
        stepSize = 1
        this.disabled = disabled
    }
}

fun RBuilder.decimalInput(value: String?, disabled: Boolean, onUpdate: (String) -> Unit) = NumericInput {
    attrs {
        this.value = value ?: "0"
        this.onValueChange = { _, valueAsString -> onUpdate(valueAsString) }
        this.disabled = disabled
    }
}

fun RBuilder.booleanInput(value: String?, disabled: Boolean, onUpdate: (String) -> Unit) = Switch {
    attrs {
        className = "object-value-input__boolean"
        inline = true
        checked = value?.toBoolean() ?: false
        onChange = { event ->
            val target = event.target.unsafeCast<HTMLInputElement>()
            onUpdate(target.checked.toString())
        }
        this.disabled = disabled
    }
}

fun RBuilder.refBookInput(value: String?, onUpdate: (String) -> Unit, aspectRefBookId: String, disabled: Boolean) = referenceBookInput {
    attrs {
        this.itemId = value
        this.refBookId = aspectRefBookId
        this.onUpdate = onUpdate
        this.disabled = disabled
    }
}
