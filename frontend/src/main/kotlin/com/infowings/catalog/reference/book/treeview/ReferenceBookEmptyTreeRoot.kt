package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.reference.book.editconsole.bookEditConsole
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookEmptyTreeRoot : RComponent<ReferenceBookEmptyTreeRoot.Props, ReferenceBookEmptyTreeRoot.State>() {

    override fun State.init(props: ReferenceBookEmptyTreeRoot.Props) {
        creatingBook = false
    }

    private fun startCreatingNewBook(e: Event) {
        setState {
            creatingBook = true
        }
        props.startCreatingNewBook(props.aspectName, e)
    }

    private fun cancelBookCreating() {
        setState {
            creatingBook = false
        }
    }


    override fun RBuilder.render() {
        val selected = props.selected
        div(classes = "book-tree-view--root") {
            div(classes = "book-tree-view--label${if (selected) " book-tree-view--label__selected" else ""}") {
                attrs {
                    onClickFunction = ::startCreatingNewBook
                }
                span(classes = "book-tree-view--label-name") {
                    +props.aspectName
                }
                +":"
                if (state.creatingBook && selected) {
                    bookEditConsole {
                        attrs {
                            book = ReferenceBookData(null, "", props.aspectId)
                            onCancel = ::cancelBookCreating
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
        var aspectId: String
        var aspectName: String
        var selected: Boolean
        var startCreatingNewBook: (aspectName: String, e: Event) -> Unit
        var submitBookChanges: (ReferenceBookData) -> Unit
    }


    interface State : RState {
        var creatingBook: Boolean
    }
}

fun RBuilder.referenceBookEmptyTreeRoot(block: RHandler<ReferenceBookEmptyTreeRoot.Props>) =
    child(ReferenceBookEmptyTreeRoot::class, block)
