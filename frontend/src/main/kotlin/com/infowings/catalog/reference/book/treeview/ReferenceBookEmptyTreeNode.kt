package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.reference.book.editconsole.bookEditConsole
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookEmptyTreeNode : RComponent<ReferenceBookEmptyTreeNode.Props, ReferenceBookEmptyTreeNode.State>() {

    override fun State.init(props: ReferenceBookEmptyTreeNode.Props) {
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

    private fun cancelCreatingBook() {
        setState {
            creatingBook = false
        }
    }

    private suspend fun handleCreateBook(book: ReferenceBook) {
        props.createBook(book)
        setState {
            creatingBook = false
        }
    }

    override fun RBuilder.render() {
        val selected = props.selected
        treeNode {
            attrs {
                treeNodeContent = buildElement {
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
                                    book = ReferenceBook(
                                        props.aspectId,
                                        "",
                                        ReferenceBookItem(props.aspectId, null, "", "", emptyList(), false, 0),
                                        false,
                                        0
                                    )
                                    onCancel = ::cancelCreatingBook
                                    onSubmit = { handleCreateBook(it) }
                                }
                            }
                        } else {
                            span(classes = "book-tree-view--empty") {
                                +"(Add Reference Book ...)"
                            }
                        }
                    }
                }!!
            }
        }
    }

    interface Props : RProps {
        var aspectId: String
        var aspectName: String
        var selected: Boolean
        var startCreatingBook: (selectedAspectName: String) -> Unit
        var createBook: suspend (ReferenceBook) -> Unit
    }

    interface State : RState {
        var creatingBook: Boolean
    }
}

fun RBuilder.referenceBookEmptyTreeNode(block: RHandler<ReferenceBookEmptyTreeNode.Props>) =
    child(ReferenceBookEmptyTreeNode::class, block)
