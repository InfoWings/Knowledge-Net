package com.infowings.catalog.reference.book

import com.infowings.catalog.aspects.getAllAspects
import com.infowings.catalog.common.ReferenceBookData
import kotlinx.coroutines.experimental.launch
import react.*
import kotlin.reflect.KClass

interface ReferenceBookApiReceiverProps : RProps {
    var loading: Boolean
    var aspectBookPairs: List<AspectBookPair>
    var onReferenceBookUpdate: (bookData: ReferenceBookData) -> Unit
    var onReferenceBookCreate: (aspectName: String, bookData: ReferenceBookData) -> Unit
}

/**
 * Component that manages already fetched aspects and makes real requests to the server API
 */
class ReferenceBookApiMiddleware : RComponent<ReferenceBookApiMiddleware.Props, ReferenceBookApiMiddleware.State>() {

    override fun State.init() {
        aspectBookPairs = emptyList()
        loading = true
    }

    override fun componentDidMount() {
        launch {
            val booksMap = getAllBooks().books
                .map { Pair(it.aspectId, it) }
                .toMap()
            val aspectBookPairs = getAllAspects().aspects
                .map { AspectBookPair(it.name, booksMap[it.id]) }

            setState {
                this.aspectBookPairs = aspectBookPairs
                loading = false
            }
        }
    }

    private fun handleCreateNewBook(aspectName: String, bookData: ReferenceBookData) {
        launch {
            if (bookData.name.isEmpty()) throw RuntimeException("Reference book name should not be empty!")

            val newBook = createBook(bookData)

            setState {
                aspectBookPairs += AspectBookPair(aspectName, newBook)
            }
        }
    }

    private fun handleUpdateAspect(bookData: ReferenceBookData) {
        launch {
            val updatedBook = updateBook(bookData)

            setState {
                aspectBookPairs = aspectBookPairs.map {
                    if (updatedBook.id == it.book?.id) AspectBookPair(it.aspectName, updatedBook) else it
                }
            }
        }
    }

    override fun RBuilder.render() {
        child(props.apiReceiverComponent) {
            attrs {
                aspectBookPairs = state.aspectBookPairs
                loading = state.loading
                onReferenceBookCreate = ::handleCreateNewBook
                onReferenceBookUpdate = ::handleUpdateAspect
            }
        }
    }

    interface Props : RProps {
        var apiReceiverComponent: KClass<out RComponent<ReferenceBookApiReceiverProps, *>>
    }

    interface State : RState {
        var aspectBookPairs: List<AspectBookPair>
        var loading: Boolean
    }
}

fun RBuilder.referenceBookApiMiddleware(apiReceiverComponent: KClass<out RComponent<ReferenceBookApiReceiverProps, *>>) =
    child(ReferenceBookApiMiddleware::class) {
        attrs {
            this.apiReceiverComponent = apiReceiverComponent
        }
    }