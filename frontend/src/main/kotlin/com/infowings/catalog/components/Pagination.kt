package com.infowings.catalog.components

import com.infowings.catalog.common.PaginationData
import com.infowings.catalog.utils.buildWithProperties
import com.infowings.catalog.wrappers.button
import com.infowings.catalog.wrappers.react.asReactElement
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.div
import kotlin.math.abs


class Pagination : RComponent<Pagination.Props, RState>() {
    companion object {
        init {
            kotlinext.js.require("styles/pagination.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "pagination-view__header") {
            div(classes = "pagination-view__pages") {
                fastBackwardButton()
                backwardButton(props.paginationData.current - 1)
                with(props) {
                    pageButtons(generatePagesIndexes(paginationData.totalPages, paginationData.pageSize, paginationData.current))
                }
                forwardButton(props.paginationData.current + 1)
                fastForwardButton()
            }
        }
    }

    private fun RBuilder.pageButtons(pages: List<Page>) {
        pages.forEach { page ->
            when (page) {
                is Page.Dots ->
                    button {
                        icon = "more"
                        disabled = true
                    }
                is Page.Numbered ->
                    button {
                        text = page.idx.toString().asReactElement()
                        onClick = { this@Pagination.props.onPageSelect(page.idx) }
                        active = page.idx == props.paginationData.current
                    }
            }
        }
    }

    private fun RBuilder.fastBackwardButton() {
        button {
            icon = "fast-backward"
            onClick = { this@Pagination.props.onPageSelect(1) }
            disabled = (props.paginationData.current == 1)
        }
    }

    private fun RBuilder.backwardButton(page: Int) {
        button {
            icon = "caret-left"
            onClick = { this@Pagination.props.onPageSelect(page) }
            disabled = (props.paginationData.current == 1)
        }
    }

    private fun RBuilder.fastForwardButton() {
        button {
            icon = "fast-forward"
            onClick = { this@Pagination.props.onPageSelect(props.paginationData.totalPages) }
            disabled = (props.paginationData.current == props.paginationData.totalPages)
        }
    }

    private fun RBuilder.forwardButton(page: Int) {
        button {
            icon = "caret-right"
            onClick = { this@Pagination.props.onPageSelect(page) }
            disabled = (props.paginationData.current == props.paginationData.totalPages)
        }
    }

    class Props(
        var paginationData: PaginationData,
        var onPageSelect: (Int) -> Unit
    ) : RProps
}

sealed class Page {
    object Dots : Page()
    class Numbered(val idx: Int) : Page()
}

private fun IntRange.toPageList() = map { Page.Numbered(it) }.toList()


internal fun generatePagesIndexes(totalPages: Int, pageSize: Int, selected: Int): List<Page> {
    require(pageSize > 0) {
        "Page size should be positive"
    }
    if (totalPages == 0) return emptyList()

    require(selected in 1..totalPages) {
        "Selected page should be in range 1..$totalPages"
    }

    val selectedUpper = selected + 2
    val selectedLower = selected - 2
    return when {
        totalPages <= 6 -> (1..totalPages).toPageList()
        selected <= 3 -> (1..selectedUpper).toPageList() + listOf(Page.Dots) + Page.Numbered(totalPages)
        selected >= totalPages - 2 -> listOf(Page.Numbered(1)) + listOf(Page.Dots) + (selectedLower..totalPages).toPageList()
        else /*selected > 3 && selected < totalPages - 1 */ -> {
            listOf(Page.Numbered(1)) +
                    dotsIfNeeded(1, selectedLower) +
                    (selectedLower..selectedUpper).toPageList() +
                    dotsIfNeeded(selectedUpper, totalPages) +
                    listOf(Page.Numbered(totalPages))
        }
    }
}

private fun dotsIfNeeded(left: Int, right: Int) = if (abs(left - right) > 1) listOf(Page.Dots) else emptyList()

fun RBuilder.paginationPanel(builder: Pagination.Props.() -> Unit) = buildWithProperties<Pagination.Props, Pagination>(builder)
