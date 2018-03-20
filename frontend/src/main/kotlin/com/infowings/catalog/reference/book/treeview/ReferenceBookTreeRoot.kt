package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.wrappers.react.use
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.svg

class ReferenceBookTreeRoot : RComponent<ReferenceBookTreeRoot.Props, ReferenceBookTreeRoot.State>() {

    private fun handleExpanderClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        setState {
            expanded = !expanded
        }
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view--root") {
            if (props.book.children.isNotEmpty()) {
                svg("aspect-tree-view--line-icon aspect-tree-view--line-icon__clickable") {
                    attrs {
                        onClickFunction = ::handleExpanderClick
                    }
                    if (state.expanded) {
                        use("svg/sprite.svg#icon-squared-minus")
                    } else {
                        use("svg/sprite.svg#icon-squared-plus")
                    }
                }
            } else {
                svg("aspect-tree-view--line-icon") {
                    use("svg/sprite.svg#icon-add-to-list")
                }
            }
            referenceBookRootLabel {
                attrs {
                    aspectName = props.aspectName
                    book = props.book
                    onClick = props.onBookClick
                    selected = props.selectedId == props.book.id
                }
            }
        }
        if (props.book.children.isNotEmpty() && state.expanded) {
            referenceBookTreeItems {
                attrs {
                    bookItems = props.book.children
                    bookContext = props.bookContext
                    onBookItemClick = props.onBookItemClick
                }
            }
        }
    }

    interface Props : RProps {
        var book: ReferenceBook
        var aspectName: String
        var onBookClick: (ReferenceBookData) -> Unit
        var onBookItemClick: (ReferenceBookItem) -> Unit
        var bookContext: Map<String, ReferenceBookData>
        var selectedId: String?
    }

    interface State : RState {
        var expanded: Boolean
    }
}

fun RBuilder.referenceBookTreeRoot(block: RHandler<ReferenceBookTreeRoot.Props>) =
    child(ReferenceBookTreeRoot::class, block)
