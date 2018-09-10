package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.reference.book.editconsole.bookEditConsole
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookEmptyNode : RComponent<ReferenceBookEmptyNode.Props, ReferenceBookEmptyNode.State>() {

    override fun State.init(props: ReferenceBookEmptyNode.Props) {
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
                    div(classes = "book-tree-view__label${if (selected) " book-tree-view__label__selected" else ""}") {
                        attrs {
                            onClickFunction = ::startCreatingBook
                        }
                        span(classes = "book-tree-view__label-name") {
                            +props.aspectName
                        }
                        span(classes = "book-tree-view__subject-name") {
                            +"(${props.subjectName ?: "Global"})"
                        }
                        +":"
                        if (state.creatingBook && selected) {
                            bookEditConsole {
                                attrs {
                                    book = ReferenceBook(
                                        props.aspectId,
                                        "",
                                        "",
                                        null,
                                        emptyList(),
                                        false,
                                        0,
                                        null
                                    )
                                    onCancel = ::cancelCreatingBook
                                    onSubmit = { handleCreateBook(it) }
                                }
                            }
                        } else {
                            span(classes = "book-tree-view__empty") {
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
        var subjectName: String?
        var selected: Boolean
        var startCreatingBook: (selectedAspectName: String) -> Unit
        var createBook: suspend (ReferenceBook) -> Unit
    }

    interface State : RState {
        var creatingBook: Boolean
    }
}

fun RBuilder.referenceBookEmptyNode(block: RHandler<ReferenceBookEmptyNode.Props>) =
    child(ReferenceBookEmptyNode::class, block)
