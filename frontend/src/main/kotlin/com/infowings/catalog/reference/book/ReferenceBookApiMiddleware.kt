package com.infowings.catalog.reference.book

import com.infowings.catalog.aspects.getAllAspects
import com.infowings.catalog.common.ReferenceBookData
import kotlinx.coroutines.experimental.launch
import react.*
import kotlin.reflect.KClass

interface ReferenceBookApiReceiverProps : RProps {
    var loading: Boolean
    var aspectBookPairs: List<RowData>
    var onReferenceBookUpdate: (name: String, bookData: ReferenceBookData) -> Unit
    var onReferenceBookCreate: (bookData: ReferenceBookData) -> Unit
}

/**
 * Component that manages already fetched books and makes real requests to the server API
 */
class ReferenceBookApiMiddleware : RComponent<ReferenceBookApiMiddleware.Props, ReferenceBookApiMiddleware.State>() {

    override fun State.init() {
        rowDataList = emptyList()
        loading = true
    }

    override fun componentDidMount() {
        launch {
            val booksMap = getAllBooks().books
                .map { Pair(it.aspectId, it) }
                .toMap()

            val rowDataList = getAllAspects().aspects
                .map { RowData(it.id!!, it.name, booksMap[it.id!!]) }

            setState {
                this.rowDataList = rowDataList
                loading = false
            }
        }
    }

    private fun handleCreateNewBook(bookData: ReferenceBookData) {
        launch {
            val newName = bookData.name
            if (newName == null || newName.isEmpty()) throw RuntimeException("Reference book name should not be empty!")

            val newBook = createBook(bookData)

            setState {
                rowDataList = rowDataList.map {
                    if (it.aspectId == bookData.aspectId) RowData(it.aspectId, it.aspectName, newBook) else it
                }
            }
        }
    }

    private fun handleUpdateAspect(name: String, bookData: ReferenceBookData) {
        launch {
            val updatedBook = updateBook(name, bookData)

            setState {
                rowDataList = rowDataList.map {
                    if (it.aspectId == bookData.aspectId) RowData(it.aspectId, it.aspectName, updatedBook) else it
                }
            }
        }
    }

    override fun RBuilder.render() {
        child(props.apiReceiverComponent) {
            attrs {
                aspectBookPairs = state.rowDataList
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
        var rowDataList: List<RowData>
        var loading: Boolean
    }
}

fun RBuilder.referenceBookApiMiddleware(apiReceiverComponent: KClass<out RComponent<ReferenceBookApiReceiverProps, *>>) =
    child(ReferenceBookApiMiddleware::class) {
        attrs {
            this.apiReceiverComponent = apiReceiverComponent
        }
    }