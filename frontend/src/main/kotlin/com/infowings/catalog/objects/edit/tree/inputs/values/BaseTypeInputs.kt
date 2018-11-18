package com.infowings.catalog.objects.edit.tree.inputs.values

import com.infowings.catalog.common.LinkValueData
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.RangeFlagConstants
import com.infowings.catalog.objects.edit.tree.inputs.entityLinkGuidInput
import com.infowings.catalog.objects.edit.tree.inputs.rangedDecimalInput
import com.infowings.catalog.objects.edit.tree.inputs.rangedNumericInput
import com.infowings.catalog.objects.edit.tree.inputs.referenceBookInput
import com.infowings.catalog.wrappers.blueprint.EditableText
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

/*
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
*/

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

fun RBuilder.rangedNumericInput(value: ObjectValueData.IntegerValue, onUpdate: (Int, Int) -> Unit, disabled: Boolean) = rangedNumericInput {
    attrs {
        this.lwb = value.value
        this.upb = value.upb
        this.onUpdate = onUpdate
        this.disabled = disabled
    }
}

fun RBuilder.rangedDecimalInput(value: ObjectValueData.DecimalValue, onUpdate: (String, String, Boolean) -> Unit, disabled: Boolean) = rangedDecimalInput {
    attrs {
        this.lwb = if (RangeFlagConstants.LEFT_INF.isSet(value.rangeFlags)) "" else value.valueRepr
        this.upb = if (RangeFlagConstants.RIGHT_INF.isSet(value.rangeFlags)) "" else value.upbRepr
        this.isRange = if (RangeFlagConstants.RANGE.isSet(value.rangeFlags)) true else value.upbRepr != value.valueRepr
        this.onUpdate = onUpdate
        this.disabled = disabled
    }
}


fun RBuilder.entityLinkInput(value: LinkValueData?, onUpdate: (LinkValueData?) -> Unit, disabled: Boolean = false) = entityLinkGuidInput {
    attrs {
        this.value = value
        this.onUpdate = onUpdate
        this.disabled = disabled
    }
}
