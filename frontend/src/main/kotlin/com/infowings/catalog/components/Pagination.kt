package com.infowings.catalog.components

import com.infowings.catalog.utils.buildWithProperties
import com.infowings.catalog.wrappers.button
import com.infowings.catalog.wrappers.react.asReactElement
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.div
import kotlin.math.abs
import kotlin.math.ceil


class Pagination : RComponent<Pagination.Props, RState>() {

    override fun RBuilder.render() {
        div(classes = "object-tree-view__header object-header") {
            div(classes = "object-header__pages") {
                fastBackwardButton()
                backwardButton(props.selected - 1)
                with(props) {
                    pageButtons(generatePagesIndexes(totalItems, pageSize, selected))
                }
                forwardButton(props.selected + 1)
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
                        active = page.idx == props.selected
                    }
            }
        }
    }

    private fun RBuilder.fastBackwardButton() {
        button {
            icon = "fast-backward"
            onClick = { this@Pagination.props.onPageSelect(1) }
            disabled = (props.selected == 1)
        }
    }

    private fun RBuilder.backwardButton(page: Int) {
        button {
            icon = "caret-left"
            onClick = { this@Pagination.props.onPageSelect(page) }
            disabled = (props.selected == 1)
        }
    }

    private fun RBuilder.fastForwardButton() {
        button {
            icon = "fast-forward"
            onClick = { this@Pagination.props.onPageSelect(totalPages(props.totalItems, props.pageSize)) }
            disabled = (props.selected == totalPages(props.totalItems, props.pageSize))
        }
    }

    private fun RBuilder.forwardButton(page: Int) {
        button {
            icon = "caret-right"
            onClick = { this@Pagination.props.onPageSelect(page) }
            disabled = (props.selected == totalPages(props.totalItems, props.pageSize))
        }
    }

    class Props(
        var pageSize: Int,
        var totalItems: Int,
        var selected: Int,
        var onPageSelect: (Int) -> Unit
    ) : RProps

}

sealed class Page {
    object Dots : Page()
    class Numbered(val idx: Int) : Page()
}

private fun IntRange.toPageList() = map { Page.Numbered(it) }.toList()


internal fun generatePagesIndexes(totalItems: Int, pageSize: Int, selected: Int): List<Page> {
    require(pageSize > 0)
    val totalPages = totalPages(totalItems, pageSize)
    require(selected in 0..totalPages)

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

private fun totalPages(totalItems: Int, pageSize: Int) = ceil(totalItems.toDouble() / pageSize).toInt()

fun RBuilder.paginationPanel(builder: Pagination.Props.() -> Unit) = buildWithProperties<Pagination.Props, Pagination>(builder)
