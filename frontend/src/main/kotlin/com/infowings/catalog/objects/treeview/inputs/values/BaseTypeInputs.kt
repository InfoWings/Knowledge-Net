package com.infowings.catalog.objects.treeview.inputs.values

import com.infowings.catalog.wrappers.blueprint.EditableText
import com.infowings.catalog.wrappers.blueprint.NumericInput
import com.infowings.catalog.wrappers.blueprint.Switch
import org.w3c.dom.HTMLInputElement
import react.RBuilder

fun RBuilder.textInput(value: String?, onUpdate: (String) -> Unit, onEdit: () -> Unit) = EditableText {
    attrs {
        this.value = value ?: ""
        placeholder = "Enter property value"
        onCancel = onUpdate
        onChange = onUpdate
        this.onEdit = onEdit
    }
}

fun RBuilder.integerInput(value: String?, onUpdate: (String) -> Unit) = NumericInput {
    attrs {
        this.value = value ?: "0"
        this.onValueChange = { valueAsNumber, _ -> onUpdate(valueAsNumber.toInt().toString()) }
        majorStepSize = 1
        minorStepSize = 1
        stepSize = 1
    }
}

fun RBuilder.decimalInput(value: String?, onUpdate: (String) -> Unit) = NumericInput {
    attrs {
        this.value = value ?: "0"
        this.onValueChange = { _, valueAsString -> onUpdate(valueAsString) }
    }
}

fun RBuilder.booleanInput(value: String?, onUpdate: (String) -> Unit) = Switch {
    attrs {
        className = "object-value-input__boolean"
        inline = true
        checked = value?.toBoolean() ?: false
        onChange = { event ->
            val target = event.target.unsafeCast<HTMLInputElement>()
            onUpdate(target.checked.toString())
        }
    }
}

class RefBookValue(val refBookId: String, val refBookTreePath: List<RefBookNodeDescriptor>)

class RefBookNodeDescriptor(val id: String, val name: String)

//fun RBuilder.refBookInput(value: RefBookValue, onUpdate: (RefBookValue) -> Unit) =
