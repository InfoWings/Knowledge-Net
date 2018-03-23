package com.infowings.catalog.reference.book

import com.infowings.catalog.aspects.getAllAspects
import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.common.ReferenceBookItemData
import kotlinx.coroutines.experimental.launch
import react.*
import kotlin.reflect.KClass


interface ReferenceBookApiReceiverProps : RProps {
    var rowDataList: List<RowData>
    var updateBook: (bookData: ReferenceBookData) -> Unit
    var createBook: (ReferenceBookData) -> Unit
    var createBookItem: (ReferenceBookItemData) -> Unit
    var updateBookItem: (ReferenceBookItemData) -> Unit
}


/**
 * Component that manages already fetched books and makes real requests to the server API
 */
class ReferenceBookApiMiddleware : RComponent<ReferenceBookApiMiddleware.Props, ReferenceBookApiMiddleware.State>() {

    override fun State.init() {
        rowDataList = emptyList()
    }

    override fun componentDidMount() {
        launch {
            val aspectIdToBookMap = getAllReferenceBooks().books
                .map { Pair(it.aspectId, it) }
                .toMap()

            val rowDataList = getAllAspects().aspects
                .map { RowData(it.id!!, it.name, aspectIdToBookMap[it.id!!]) }

            setState {
                this.rowDataList = rowDataList
            }
        }
    }

    private fun createBook(bookData: ReferenceBookData) {
        launch {
            /*
            Maybe get ReferenceBook is not optimal way.
            Actually we need only created ReferenceBook id.
            */
            val newBook = createReferenceBook(bookData)
            updateRowDataList(bookData.aspectId, newBook)
        }
    }

    private fun updateBook(bookData: ReferenceBookData) {
        launch {
            /*
            Maybe get ReferenceBook with all his children is not optimal way, because it can be very large json
            Actually we need only to know is updating was successful.
            */
            val updatedBook = updateReferenceBook(bookData)
            updateRowDataList(bookData.aspectId, updatedBook)
        }
    }

    private fun createBookItem(bookItemData: ReferenceBookItemData) {
        launch {
            /*
            Maybe get ReferenceBook with all his children is not optimal way, because it can be very large json
            Actually we need only created ReferenceBookItem id.
            */
            val updatedBook = createReferenceBookItem(bookItemData)
            updateRowDataList(updatedBook.aspectId, updatedBook)
        }
    }

    private fun updateBookItem(bookItemData: ReferenceBookItemData) {
        launch {
            /*
            Maybe get ReferenceBook with all his children is not optimal way, because it can be very large json
            Actually we need only to know is updating was successful.
            */
            val updatedBook = updateReferenceBookItem(bookItemData)
            updateRowDataList(updatedBook.aspectId, updatedBook)
        }
    }

    private fun updateRowDataList(aspectId: String, book: ReferenceBook) {
        setState {
            rowDataList = rowDataList.map {
                if (it.aspectId == aspectId) it.copy(book = book) else it
            }
        }
    }


    override fun RBuilder.render() {
        child(props.apiReceiverComponent) {
            attrs {
                rowDataList = state.rowDataList
                createBook = ::createBook
                updateBook = ::updateBook
                createBookItem = ::createBookItem
                updateBookItem = ::updateBookItem
            }
        }
    }

    interface Props : RProps {
        var apiReceiverComponent: KClass<out RComponent<ReferenceBookApiReceiverProps, *>>
    }

    interface State : RState {
        var rowDataList: List<RowData>
    }
}

fun RBuilder.referenceBookApiMiddleware(apiReceiverComponent: KClass<out RComponent<ReferenceBookApiReceiverProps, *>>) =
    child(ReferenceBookApiMiddleware::class) {
        attrs {
            this.apiReceiverComponent = apiReceiverComponent
        }
    }

data class RowData(val aspectId: String, val aspectName: String, val book: ReferenceBook?)