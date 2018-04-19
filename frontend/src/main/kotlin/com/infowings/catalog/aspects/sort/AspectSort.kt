package com.infowings.catalog.aspects.sort

import com.infowings.catalog.common.AspectOrderBy
import com.infowings.catalog.common.AspectSortField
import com.infowings.catalog.common.Direction
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.ButtonGroup
import kotlinext.js.require
import react.*
import react.dom.div


class AspectSort : RComponent<AspectSort.Props, AspectSort.State>() {

    companion object {
        init {
            require("styles/aspect-sort.scss")
        }
    }

    override fun AspectSort.State.init() {
        orderBy = mutableListOf(AspectOrderBy(AspectSortField.NAME, Direction.ASC))
    }

    override fun RBuilder.render() {
        div(classes = "sort-button-group-container") {
            ButtonGroup {
                Button {
                    attrs {
                        icon = curAspectNameSortModeIcon(AspectSortField.NAME)
                        onClick = { onClickSort(AspectSortField.NAME) }
                    }
                    +"Name${getOrderByFieldOrder(AspectSortField.NAME)}"
                }
                Button {
                    attrs {
                        icon = curAspectNameSortModeIcon(AspectSortField.SUBJECT)
                        onClick = { onClickSort(AspectSortField.SUBJECT) }
                    }
                    +"Subject${getOrderByFieldOrder(AspectSortField.SUBJECT)}"
                }
            }
        }
    }

    private fun onClickSort(field: AspectSortField) {
        setState {
            val curOrder = orderBy.find { it.name == field }
            if (curOrder == null)
                orderBy.add(AspectOrderBy(field, Direction.ASC))
            else {
                if (curOrder.direction == Direction.ASC) {
                    orderBy.add(AspectOrderBy(field, Direction.DESC))
                }
                orderBy.remove(curOrder)
            }
            props.onFetchAspect(orderBy)
        }
    }

    private fun getOrderByFieldOrder(field: AspectSortField) =
        state.orderBy.indexOfFirst { it.name == field }.let { if (it == -1) "" else " (${it + 1})" }

    private fun curAspectNameSortModeIcon(field: AspectSortField): String {
        val directToIcon = mapOf(Direction.ASC to "sort-alphabetical", Direction.DESC to "sort-alphabetical-desc")
        return state.orderBy.find { it.name == field }?.let { directToIcon[it.direction] ?: "double-caret-vertical" }
                ?: "double-caret-vertical"
    }

    interface Props : RProps {
        var onFetchAspect: (List<AspectOrderBy>) -> Unit
    }

    interface State : RState {
        var orderBy: MutableList<AspectOrderBy>
    }
}


fun RBuilder.aspectSort(block: RHandler<AspectSort.Props>) = child(AspectSort::class, block)