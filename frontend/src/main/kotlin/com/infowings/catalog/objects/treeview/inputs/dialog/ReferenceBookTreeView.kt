package com.infowings.catalog.objects.treeview.inputs.dialog

import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.treeview.inputs.values.RefBookNodeDescriptor
import react.*

class ReferenceBookItemListView : RComponent<ReferenceBookItemListView.Props, RState>() {
    override fun RBuilder.render() {
        props.referenceBookItemList.forEachIndexed { index, referenceBookItem ->
            child(ReferenceBookTreeView::class) {
                attrs {
                    referenceBookTreeViewModel = referenceBookItem
                    selectedPath = props.selectedPath?.let { if (it.first().id == referenceBookItem.id) it else null }
                    onUpdate = { block ->
                        props.onUpdate(index, block)
                    }
                    onSelect = props.onSelect
                }
            }
        }
    }

    interface Props : RProps {
        var referenceBookItemList: List<ReferenceBookItemViewModel>
        var selectedPath: List<RefBookNodeDescriptor>?
        var onUpdate: (Int, ReferenceBookItemViewModel.() -> Unit) -> Unit
        var onSelect: (List<RefBookNodeDescriptor>) -> Unit
    }
}

fun RBuilder.referenceBookListView(handler: RHandler<ReferenceBookItemListView.Props>) = child(ReferenceBookItemListView::class, handler)

class ReferenceBookTreeView : RComponent<ReferenceBookTreeView.Props, RState>() {

    private fun handleUpdateAtIndex(index: Int, updater: ReferenceBookItemViewModel.() -> Unit) = props.onUpdate {
        children[index].updater()
    }

    private fun handleSelectPath(childPath: List<RefBookNodeDescriptor>) =
        props.onSelect(childPath + RefBookNodeDescriptor(props.referenceBookTreeViewModel.id, props.referenceBookTreeViewModel.value))

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

                }!!
            }
            child(ReferenceBookItemListView::class) {
                attrs {
                    referenceBookItemList = props.referenceBookTreeViewModel.children
                    selectedPath = props.selectedPath?.drop(1)
                    onUpdate = this@ReferenceBookTreeView::handleUpdateAtIndex
                    onSelect = this@ReferenceBookTreeView::handleSelectPath
                }
            }
        }
    }

    interface Props : RProps {
        var referenceBookTreeViewModel: ReferenceBookItemViewModel
        var selectedPath: List<RefBookNodeDescriptor>?
        var onUpdate: (ReferenceBookItemViewModel.() -> Unit) -> Unit
        var onSelect: (List<RefBookNodeDescriptor>) -> Unit
    }
}

fun RBuilder.referenceBookTreeView(handler: RHandler<ReferenceBookTreeView.Props>) = child(ReferenceBookTreeView::class, handler)
