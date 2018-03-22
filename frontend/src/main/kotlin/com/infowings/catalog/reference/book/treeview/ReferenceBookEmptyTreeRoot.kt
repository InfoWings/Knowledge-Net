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

    private fun startCreatingBook(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        setState {
            creatingBook = true
        }
        props.startCreatingBook(props.aspectName)
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
                    onClickFunction = ::startCreatingBook
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
                            onSubmit = props.createBook
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
        var startCreatingBook: (selectedAspectName: String) -> Unit
        var createBook: (ReferenceBookData) -> Unit
    }

    interface State : RState {
        var creatingBook: Boolean
    }
}

fun RBuilder.referenceBookEmptyTreeRoot(block: RHandler<ReferenceBookEmptyTreeRoot.Props>) =
    child(ReferenceBookEmptyTreeRoot::class, block)
