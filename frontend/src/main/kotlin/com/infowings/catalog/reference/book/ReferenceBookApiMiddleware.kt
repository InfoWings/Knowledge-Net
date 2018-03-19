package com.infowings.catalog.reference.book

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
import kotlinx.coroutines.experimental.launch
import react.*
import kotlin.reflect.KClass

interface ReferenceBookApiReceiverProps : RProps {
    var aspectId: String
    var loading: Boolean
    var books: List<ReferenceBook>
    var onReferenceBookUpdate: (changedAspect: ReferenceBookData) -> Unit
    var onReferenceBookCreate: (newAspect: ReferenceBookData) -> Unit
    var onClosePopup: () -> Unit
}

/**
 * Component that manages already fetched aspects and makes real requests to the server API
 */
class ReferenceBookApiMiddleware : RComponent<ReferenceBookApiMiddleware.Props, ReferenceBookApiMiddleware.State>() {

    override fun State.init() {
        books = emptyList()
        loading = true
    }

    override fun componentDidMount() {
        launch {
            val referenceBooks = getByAspectId(props.aspectId).books
            console.log(referenceBooks.size)
            setState {
                books = referenceBooks
                loading = false
            }
        }
    }

    private fun handleCreateNewBook(bookData: ReferenceBookData) {
        launch {
            if (bookData.name.isEmpty()) throw RuntimeException("Reference book name should not be empty!")

            val newBook = create(bookData)

            setState {
                books += newBook
            }
        }
    }

    private fun handleUpdateAspect(bookData: ReferenceBookData) {
        launch {
            val updatedBook = update(bookData)

            setState {
                books = books.map {
                    if (updatedBook.id == it.id) updatedBook else it
                }
            }
        }
    }

    override fun RBuilder.render() {
        child(props.apiReceiverComponent) {
            attrs {
                aspectId = props.aspectId
                books = state.books
                loading = state.loading
                onReferenceBookCreate = ::handleCreateNewBook
                onReferenceBookUpdate = ::handleUpdateAspect
                onClosePopup = props.aspectUnselected
            }
        }
    }

    interface Props : RProps {
        var aspectId: String
        var aspectUnselected: () -> Unit
        var apiReceiverComponent: KClass<out RComponent<ReferenceBookApiReceiverProps, *>>
    }

    interface State : RState {
        /**
         * Last fetched books from server (actual)
         */
        var books: List<ReferenceBook>
        /**
         * Flag showing if the books is still being fetched
         */
        var loading: Boolean
    }
}

fun RBuilder.referenceBookApiMiddleware(
    aspectId: String,
    aspectUnselected: () -> Unit,
    apiReceiverComponent: KClass<out RComponent<ReferenceBookApiReceiverProps, *>>
) =
    child(ReferenceBookApiMiddleware::class) {
        attrs {
            this.aspectId = aspectId
            this.aspectUnselected = aspectUnselected
            this.apiReceiverComponent = apiReceiverComponent
        }
    }