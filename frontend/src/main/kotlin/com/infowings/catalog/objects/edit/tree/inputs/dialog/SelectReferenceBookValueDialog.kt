package com.infowings.catalog.objects.edit.tree.inputs.dialog

import com.infowings.catalog.common.RefBookNodeDescriptor
import com.infowings.catalog.objects.edit.tree.inputs.RefBookValue
import com.infowings.catalog.reference.book.getReferenceBookById
import com.infowings.catalog.reference.book.getReferenceBookItemPath
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Callout
import com.infowings.catalog.wrappers.blueprint.Dialog
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.react.asReactElement
import kotlinext.js.require
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import react.*
import react.dom.div

class SelectReferenceBookValueDialog(props: Props) :
    RComponent<SelectReferenceBookValueDialog.Props, SelectReferenceBookValueDialog.State>(props), JobCoroutineScope by JobSimpleCoroutineScope() {

    companion object {
        init {
            require("styles/reference-book-dialog.scss")
        }
    }

    override fun componentWillMount() {
        job = Job()
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun State.init(props: Props) {
        selectedValue = props.initialValue
    }

    override fun componentDidUpdate(prevProps: Props, prevState: State, snapshot: Any) {
        if (!prevProps.isOpen && props.isOpen) {
            launch {
                val referenceBook = getReferenceBookById(props.initialValue.refBookId)
                setState {
                    referenceBookViewModel =
                            referenceBook.toSelectViewModel().expandPath(props.initialValue.refBookTreePath)
                }
            }
        }
    }

    private fun selectItem(itemId: String) {
        launch {
            val selectedPath = getReferenceBookItemPath(itemId).path
            setState {
                selectedValue = RefBookValue(state.selectedValue.refBookId, selectedPath)
            }
        }
    }

    private fun handleUpdateModel(index: Int, block: ReferenceBookItemViewModel.() -> Unit) = setState {
        val model = referenceBookViewModel ?: return@setState
        model.items[index].block()
    }

    private fun handleConfirmSelect() = props.onSelect(state.selectedValue)

    override fun RBuilder.render() {
        Dialog {
            attrs {
                isOpen = props.isOpen
                onClose = { props.onCancel() }
                title = "Select value from reference book${state.referenceBookViewModel?.let { " ${it.name}" }}".asReactElement()
            }
            div(classes = "bp3-dialog-body") {
                if (state.selectedValue.refBookTreePath.isNotEmpty()) {
                    referenceBookPathView(path = state.selectedValue.refBookTreePath)
                }
                state.referenceBookViewModel?.let {
                    referenceBookListView {
                        attrs {
                            referenceBookItemList = it.items
                            selectedPath = state.selectedValue.refBookTreePath
                            onSelect = this@SelectReferenceBookValueDialog::selectItem
                            onUpdate = this@SelectReferenceBookValueDialog::handleUpdateModel
                        }
                    }
                }
            }
            div(classes = "bp3-dialog-footer") {
                div(classes = "bp3-dialog-footer-actions") {
                    Button {
                        attrs {
                            intent = Intent.PRIMARY
                            text = "Apply value".asReactElement()
                            onClick = { handleConfirmSelect() }
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
        var selectedValue: RefBookValue
        var referenceBookViewModel: ReferenceBookViewModel?
    }
}

fun RBuilder.referenceBookPathView(path: List<RefBookNodeDescriptor>) = Callout {
    +path.joinToString(" → ") { it.value }
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