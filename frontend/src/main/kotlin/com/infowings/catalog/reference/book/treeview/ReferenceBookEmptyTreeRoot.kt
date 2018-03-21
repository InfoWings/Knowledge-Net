package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.reference.book.editconsole.bookEditConsole
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookEmptyTreeRoot : RComponent<ReferenceBookEmptyTreeRoot.Props, RState>() {

    private fun startCreatingNewBook(e: Event) {
        props.startCreatingNewBook(props.aspectName, e)
    }

    override fun RBuilder.render() {
        val selected = props.selectedAspectName == props.aspectName
        div(classes = "book-tree-view--root") {
            div(classes = "book-tree-view--label${if (selected) " book-tree-view--label__selected" else ""}") {
                attrs {
                    onClickFunction = ::startCreatingNewBook
                }
                span(classes = "book-tree-view--label-name") {
                    +props.aspectName
                }
                +":"
                if (props.creatingNewBook && selected) {
                    bookEditConsole {
                        attrs {
                            book = ReferenceBookData(null, "", props.aspectId)
                            onCancel = props.cancelBookCreating
                            onSubmit = props.submitBookChanges
                        }
                    }
                } else {
                    span(classes = "book-tree-view--empty") {
                        +"(Add Reference Book ...)"
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var creatingNewBook: Boolean
        var aspectId: String
        var aspectName: String
        var selectedAspectName: String?
        var startCreatingNewBook: (aspectName: String, e: Event) -> Unit
        var cancelBookCreating: () -> Unit
        var submitBookChanges: (ReferenceBookData) -> Unit
    }
}

fun RBuilder.referenceBookEmptyTreeRoot(block: RHandler<ReferenceBookEmptyTreeRoot.Props>) =
    child(ReferenceBookEmptyTreeRoot::class, block)
