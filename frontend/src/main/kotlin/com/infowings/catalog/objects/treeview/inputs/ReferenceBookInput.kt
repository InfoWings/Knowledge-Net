package com.infowings.catalog.objects.treeview.inputs

import com.infowings.catalog.objects.treeview.inputs.dialog.selectReferenceBookValueDialog
import com.infowings.catalog.objects.treeview.inputs.values.RefBookNodeDescriptor
import com.infowings.catalog.objects.treeview.inputs.values.RefBookValue
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.react.asReactElement
import react.*

class ReferenceBookInput : RComponent<ReferenceBookInput.Props, ReferenceBookInput.State>() {

    private fun handleClick() = setState {
        isDialogOpen = true
    }

    private fun handleCloseDialog() = setState {
        isDialogOpen = false
    }

    private fun handleConfirmSelectNewValue(newValue: RefBookValue) {
        props.onUpdate(newValue.refBookTreePath.last().id)
        setState {
            isDialogOpen = false
        }
    }

    override fun RBuilder.render() {
        if (props.value.refBookTreePath.isEmpty()) {
            emptyReferenceBookInput(
                onClick = this@ReferenceBookInput::handleClick
            )
        } else {
            valueReferenceBookInput(
                renderedPath = props.value.refBookTreePath,
                onClick = this@ReferenceBookInput::handleClick
            )
        }
        selectReferenceBookValueDialog(
            isOpen = state.isDialogOpen,
            initialValue = props.value,
            onSelect = this@ReferenceBookInput::handleConfirmSelectNewValue,
            onCancel = this@ReferenceBookInput::handleCloseDialog
        )
    }

    interface Props : RProps {
        var value: RefBookValue
        var onUpdate: (String) -> Unit
    }

    interface State : RState {
        var isDialogOpen: Boolean
    }
}

fun RBuilder.emptyReferenceBookInput(onClick: () -> Unit) = Button {
    attrs {
        text = "Select value".asReactElement()
        intent = Intent.NONE
        this.onClick = { onClick() }
    }
}

fun RBuilder.valueReferenceBookInput(renderedPath: List<RefBookNodeDescriptor>, onClick: () -> Unit) = Button {
    attrs {
        text = renderedPath.joinToString(" -> ") { it.name }.asReactElement()
        intent = Intent.NONE
        this.onClick = { onClick() }
    }
}

fun RBuilder.referenceBookInput(handler: RHandler<ReferenceBookInput.Props>) = child(ReferenceBookInput::class, handler)