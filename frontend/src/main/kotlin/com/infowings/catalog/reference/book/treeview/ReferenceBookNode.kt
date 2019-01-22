package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.BadRequestCode.NEED_CONFIRMATION
import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.ReferenceBookItemData
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.popup.forceRemoveConfirmWindow
import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.reference.book.RefBookBadRequestException
import com.infowings.catalog.reference.book.editconsole.bookItemEditConsole
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import com.infowings.catalog.utils.addToListIcon
import com.infowings.catalog.utils.ripIcon
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div

class ReferenceBookNode : RComponent<ReferenceBookNode.Props, ReferenceBookNode.State>(), JobCoroutineScope by JobSimpleCoroutineScope() {

    override fun componentWillMount() {
        job = Job()
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun State.init() {
        creatingBookItem = false
        confirmation = false
    }

    private fun startCreatingBookItem(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        setState {
            creatingBookItem = true
        }
    }

    private fun cancelCreatingBookItem() {
        setState {
            creatingBookItem = false
        }
    }

    private suspend fun handleCreateBookItem(bookItem: ReferenceBookItem) {
        props.createBookItem(props.aspectId, ReferenceBookItemData(props.book.id, bookItem))
        setState {
            creatingBookItem = false
        }
    }

    private fun handleDeleteClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        tryDelete(false)
    }

    private fun tryDelete(force: Boolean) {
        launch {
            try {
                props.deleteBook(props.book, force)
                setState {
                    confirmation = false
                }
            } catch (e: RefBookBadRequestException) {
                when (e.exceptionInfo.code) {
                    NEED_CONFIRMATION -> setState {
                        confirmation = true
                    }
                    else -> throw e
                }
            }
        }
    }

    override fun RBuilder.render() {

        val notDeletedBookItems = props.book.children.filter { !it.deleted }

        treeNode {
            attrs {
                expanded = state.creatingBookItem
                treeNodeContent = buildElement {
                    div(classes = "book-tree-view__book") {
                        referenceBookLabel {
                            attrs {
                                aspectName = props.aspectName
                                subjectName = props.subjectName
                                book = props.book
                                startUpdatingBook = props.startUpdatingBook
                                updateBook = props.updateBook
                                selected = props.selected
                            }
                        }

                        descriptionComponent(
                            className = "book-tree-view__description",
                            description = props.book.description,
                            onNewDescriptionConfirmed = {
                                launch {
                                    props.updateBook(
                                        props.book.copy(description = it)
                                    )
                                }
                            },
                            onEditStarted = null
                        )

                        addToListIcon(classes = "book-tree-view__add-to-list-icon") {
                            attrs {
                                onClickFunction = ::startCreatingBookItem
                            }
                        }

                        ripIcon("book-tree-view__delete-icon book-tree-view__delete-icon--red") {
                            attrs {
                                onClickFunction = ::handleDeleteClick
                            }
                        }

                        if (state.confirmation) {
                            forceRemoveConfirmWindow {
                                attrs {
                                    onConfirm = { tryDelete(true) }
                                    onCancel = { setState { confirmation = false } }
                                    isOpen = state.confirmation
                                    message = "There is reference book item which has linked entities."
                                }
                            }
                        }
                    }
                }!!
            }

            if (notDeletedBookItems.isNotEmpty()) {
                notDeletedBookItems.forEach { bookItem ->
                    referenceBookItemNode {
                        attrs {
                            key = bookItem.id
                            aspectId = props.aspectId
                            book = props.book
                            this.bookItem = bookItem
                            createBookItem = props.createBookItem
                            updateBookItem = props.updateBookItem
                            deleteBookItem = props.deleteBookItem
                        }
                    }
                }
            }

            if (state.creatingBookItem) {
                bookItemEditConsole {
                    attrs {
                        bookItem = ReferenceBookItem("", "", null, emptyList(), false, 0, null)
                        onCancel = ::cancelCreatingBookItem
                        onSubmit = { bookItem, _ -> handleCreateBookItem(bookItem) }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspectId: String
        var aspectName: String
        var book: ReferenceBook
        var subjectName: String?
        var selected: Boolean
        var startUpdatingBook: (aspectName: String) -> Unit
        var updateBook: suspend (ReferenceBook) -> Unit
        var deleteBook: suspend (ReferenceBook, force: Boolean) -> Unit
        var createBookItem: suspend (aspectId: String, ReferenceBookItemData) -> Unit
        var updateBookItem: suspend (aspectId: String, ReferenceBookItem, force: Boolean) -> Unit
        var deleteBookItem: suspend (aspectId: String, ReferenceBookItem, force: Boolean) -> Unit
    }

    interface State : RState {
        var creatingBookItem: Boolean
        var confirmation: Boolean
    }
}

fun RBuilder.referenceBookNode(block: RHandler<ReferenceBookNode.Props>) =
    child(ReferenceBookNode::class, block)
