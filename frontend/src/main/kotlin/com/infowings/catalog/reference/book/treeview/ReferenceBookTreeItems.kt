package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import react.*
import react.dom.div

class ReferenceBookTreeItems : RComponent<ReferenceBookTreeItems.Props, RState>() {

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view--properties-block") {
            props.bookItems.map {
                referenceBookTreeItem {
                    attrs {
                        key = it.id
                        bookItem = it
                        onAspectPropertyClick = props.onBookItemClick
                        aspectContext = props.bookContext
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var bookItems: List<ReferenceBookItem>
        var bookContext: Map<String, ReferenceBook>
        var onBookItemClick: (ReferenceBookItem) -> Unit
    }

}

fun RBuilder.referenceBookTreeItems(block: RHandler<ReferenceBookTreeItems.Props>) = child(
    ReferenceBookTreeItems::class, block
)