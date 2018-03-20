package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.reference.book.editconsole.bookEditConsole
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookEmptyTreeRoot : RComponent<ReferenceBookEmptyTreeRoot.Props, ReferenceBookEmptyTreeRoot.State>() {

    private fun submitBookChanges(bookData: ReferenceBookData) {
        props.submitBookChanges(props.aspectName, bookData)
    }

    override fun RBuilder.render() {
        val selectedBookId = props.selectedId
        div(classes = "aspect-tree-view--root") {
            if (props.creatingNewBook) {
                bookEditConsole {
                    attrs {
                        book = ReferenceBookData(null, "", "#25:0")
                        onCancel = props.cancelBookCreating
                        onSubmit = ::submitBookChanges
                    }
                }
            } else {
                div(classes = "aspect-tree-view--label${if (selectedBookId == null) " aspect-tree-view--label__selected" else ""}") {
                    attrs {
                        onClickFunction = props.startCreatingNewBook
                    }
                    span(classes = "aspect-tree-view--label-name") {
                        +props.aspectName
                    }
                    span(classes = "aspect-tree-view--empty") {
                        +"Add Reference Book ..."
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var creatingNewBook: Boolean
        var aspectName: String
        var selectedId: String?
        var startCreatingNewBook: (Event) -> Unit
        var cancelBookCreating: () -> Unit
        var submitBookChanges: (String, ReferenceBookData) -> Unit
    }

    interface State : RState {
    }
}

fun RBuilder.referenceBookEmptyTreeRoot(block: RHandler<ReferenceBookEmptyTreeRoot.Props>) =
    child(ReferenceBookEmptyTreeRoot::class, block)
