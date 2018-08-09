package com.infowings.catalog.objects.edit.tree.inputs.dialog

import com.infowings.catalog.common.RefBookNodeDescriptor
import com.infowings.catalog.components.treeview.controlledTreeNode
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.span

val referenceBookListView = rFunction<ReferenceBookItemListViewProps>("ReferenceBookListView") { props ->
    props.referenceBookItemList.forEachIndexed { index, referenceBookItem ->
        if (!referenceBookItem.deleted) {
            child(ReferenceBookTreeView::class) {
                attrs {
                    referenceBookTreeViewModel = referenceBookItem
                    selectedPath =
                            props.selectedPath?.let { if (it.firstOrNull()?.id == referenceBookItem.id) it else null }
                    onUpdate = { block ->
                        props.onUpdate(index, block)
                    }
                    onSelect = props.onSelect
                }
            }
        }
    }
}

interface ReferenceBookItemListViewProps : RProps {
    var referenceBookItemList: List<ReferenceBookItemViewModel>
    var selectedPath: List<RefBookNodeDescriptor>?
    var onUpdate: (Int, ReferenceBookItemViewModel.() -> Unit) -> Unit
    var onSelect: (itemId: String) -> Unit
}

class ReferenceBookTreeView : RComponent<ReferenceBookTreeView.Props, RState>() {

    private fun handleUpdateAtIndex(index: Int, updater: ReferenceBookItemViewModel.() -> Unit) = props.onUpdate {
        children[index].updater()
    }

    override fun RBuilder.render() {
        controlledTreeNode {
            attrs {
                className = "refbook-tree-dialog"
                expanded = props.referenceBookTreeViewModel.isExpanded
                onExpanded = {
                    props.onUpdate {
                        this.isExpanded = it
                    }
                }
                treeNodeContent = buildElement {
                    referenceBookNode(
                        name = props.referenceBookTreeViewModel.value,
                        selected = props.selectedPath != null,
                        onClick = { props.onSelect(props.referenceBookTreeViewModel.id) }
                    )
                }!!
            }
            if (props.referenceBookTreeViewModel.children.isNotEmpty()) {
                referenceBookListView {
                    attrs {
                        referenceBookItemList = props.referenceBookTreeViewModel.children
                        selectedPath = props.selectedPath?.drop(1)
                        onUpdate = this@ReferenceBookTreeView::handleUpdateAtIndex
                        onSelect = props.onSelect
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var referenceBookTreeViewModel: ReferenceBookItemViewModel
        var selectedPath: List<RefBookNodeDescriptor>?
        var onUpdate: (ReferenceBookItemViewModel.() -> Unit) -> Unit
        var onSelect: (itemId: String) -> Unit
    }
}

fun RBuilder.referenceBookNode(name: String, selected: Boolean, onClick: () -> Unit) =
    span(classes = "refbook-dialog-node${if (selected) "__selected" else ""}") {
        attrs {
            onClickFunction = { onClick() }
        }
        +name
    }