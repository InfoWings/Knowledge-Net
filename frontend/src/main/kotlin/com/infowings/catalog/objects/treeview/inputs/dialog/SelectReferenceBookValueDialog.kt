package com.infowings.catalog.objects.treeview.inputs.dialog

import com.infowings.catalog.objects.treeview.inputs.values.RefBookNodeDescriptor
import com.infowings.catalog.objects.treeview.inputs.values.RefBookValue
import com.infowings.catalog.reference.book.getReferenceBook
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Dialog
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.react.asReactElement
import kotlinx.coroutines.experimental.launch
import react.*
import react.dom.div

class SelectReferenceBookValueDialog : RComponent<SelectReferenceBookValueDialog.Props, SelectReferenceBookValueDialog.State>() {

    override fun componentDidUpdate(prevProps: Props, prevState: State) {
        if (!prevProps.isOpen && props.isOpen) {
            launch {
                val referenceBook = getReferenceBook(props.initialValue.refBookId)
                setState {
                    referenceBookViewModel = referenceBook.toSelectViewModel().expandPath(props.initialValue.refBookTreePath)
                }
            }
        }
    }

    private fun selectPath(path: List<RefBookNodeDescriptor>) = setState {
        selectedPath = path
    }

    private fun handleUpdateModel(index: Int, block: ReferenceBookItemViewModel.() -> Unit) = setState {
        val model = referenceBookViewModel ?: return@setState
        model.items[index].block()
    }

    override fun RBuilder.render() {
        Dialog {
            attrs {
                isOpen = props.isOpen
                onClose = { }
                title = "Select value from reference book${state.referenceBookViewModel?.let { " ${it.name}" }}".asReactElement()
            }
            div(classes = "pt-dialog-body") {
                referenceBookListView {
                    attrs {
                        referenceBookItemList = state.referenceBookViewModel?.items ?: TODO("SOMETHININGIDJIFDII")
                        selectedPath = props.initialValue.refBookTreePath
                        onSelect = this@SelectReferenceBookValueDialog::selectPath
                        onUpdate = this@SelectReferenceBookValueDialog::handleUpdateModel
                    }
                }
            }
            div(classes = "pt-dialog-footer") {
                div(classes = "pt-dialog-footer-actions") {
                    Button {
                        attrs {
                            intent = Intent.PRIMARY
                            text = "Apply value".asReactElement()
                            onClick = { }
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var isOpen: Boolean
        var initialValue: RefBookValue
        var onSelect: (RefBookValue) -> Unit
        var onCancel: () -> Unit
    }

    interface State : RState {
        var selectedPath: List<RefBookNodeDescriptor>
        var referenceBookViewModel: ReferenceBookViewModel?
    }
}

fun RBuilder.selectReferenceBookValueDialog(
    isOpen: Boolean,
    initialValue: RefBookValue,
    onSelect: (RefBookValue) -> Unit,
    onCancel: () -> Unit
) = child(SelectReferenceBookValueDialog::class) {
    attrs.isOpen = isOpen
    attrs.initialValue = initialValue
    attrs.onSelect = onSelect
    attrs.onCancel = onCancel
}