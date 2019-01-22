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
                backwardButton()
                with(props) {
                    pageButtons(generatePagesIndexes(totalItems, pageSize, selected))
                }
                forwardButton()
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

    private fun RBuilder.backwardButton() {
        button {
            icon = "fast-backward"
            onClick = { this@Pagination.props.prevPage() }
            disabled = (props.selected == 1)
        }
    }

    private fun RBuilder.forwardButton() {
        button {
            icon = "fast-forward"
            onClick = { this@Pagination.props.nextPage() }
            disabled = (props.selected == totalPages(props.totalItems, props.pageSize))
        }
    }

    class Props(
        var pageSize: Int,
        var totalItems: Int,
        var selected: Int,
        var onPageSelect: (Int) -> Unit,
        var nextPage: () -> Unit,
        var prevPage: () -> Unit
    ) : RProps

}

sealed class Page {
    object Dots : Page()
    class Numbered(val idx: Int) : Page()
}

private fun IntRange.toPageList() = map { Page.Numbered(it) }.toList()

private const val leftButtons = 3
// count starts from 1 so to include last element subtract 1
private const val rightButtons = leftButtons - 1

internal fun generatePagesIndexes(totalItems: Int, pageSize: Int, selected: Int): List<Page> {
    require(pageSize > 0)
    val totalPages = totalPages(totalItems, pageSize)
    require(selected in 0..totalPages)
    val rightBound = totalPages - rightButtons

    return when {
        totalPages <= leftButtons * 2 -> return (1..totalPages).toPageList()
        selected <= leftButtons || selected >= rightBound -> (1..leftButtons).toPageList() + listOf(Page.Dots) + (rightBound..totalPages).toPageList()
        else ->
            (1..leftButtons).toPageList() +
                    dotsIfNeeded(leftButtons, selected) +
                    listOf(Page.Numbered(selected)) +
                    dotsIfNeeded(selected, rightBound) +
                    (rightBound..totalPages).toPageList()
    }
}

private fun dotsIfNeeded(left: Int, right: Int) = if (abs(left - right) > 1) listOf(Page.Dots) else emptyList()

private fun totalPages(totalItems: Int, pageSize: Int) = ceil(totalItems.toDouble() / pageSize).toInt()

fun RBuilder.paginationPanel(builder: Pagination.Props.() -> Unit) = buildWithProperties<Pagination.Props, Pagination>(builder)
