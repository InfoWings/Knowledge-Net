package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import kotlinext.js.invoke
import kotlinext.js.require
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookTreeView(props: Props) :
    RComponent<ReferenceBookTreeView.Props, ReferenceBookTreeView.State>(props) {

    companion object {
        init {
            require("styles/aspect-tree-view.scss")
        }
    }

    override fun State.init(props: Props) {
        buildingNewAspect = props.books.isEmpty()
    }

    private fun createNewBookHandler(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        props.onNewBookRequest()
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view") {
            props.books.map { book ->
                referenceBookTreeRoot {
                    attrs {
                        key = book.id
                        this.book = book
                        selectedId = props.selectedId
                        onBookClick = props.onBookClick
                        onBookItemClick = props.onBookItemClick
                        bookContext = props.bookContext
                    }
                }
            }
            div(classes = "aspect-tree-view--root") {
                div(classes = "aspect-tree-view--label${if (props.selectedId == null) " aspect-tree-view--label__selected" else ""}") {
                    attrs {
                        onClickFunction = ::createNewBookHandler
                    }
                    span(classes = "aspect-tree-view--empty") {
                        +"(Add Reference Book ...)"
                    }
                }
            }
        }
    }

    interface State : RState {
        var buildingNewAspect: Boolean
    }

    interface Props : RProps {
        var books: List<ReferenceBook>
        var onBookClick: (ReferenceBook) -> Unit
        var onBookItemClick: (ReferenceBookItem) -> Unit
        var bookContext: Map<String, ReferenceBook>
        var onNewBookRequest: () -> Unit
        var selectedId: String?
        var onNewBookItemRequest: (AspectData) -> Unit
    }
}

fun RBuilder.referenceBookTreeView(block: RHandler<ReferenceBookTreeView.Props>) =
    child(ReferenceBookTreeView::class, block)