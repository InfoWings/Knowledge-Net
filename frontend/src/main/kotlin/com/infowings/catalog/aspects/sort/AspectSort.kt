package com.infowings.catalog.aspects.sort

import com.infowings.catalog.common.SortOrder
import com.infowings.catalog.common.SortField
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
        orderBy = mutableListOf()
    }

    override fun RBuilder.render() {
        div(classes = "sort-button-group-container") {
            ButtonGroup {
                Button {
                    attrs {
                        icon = curAspectNameSortModeIcon(SortField.NAME)
                        onClick = { onClickSort(SortField.NAME) }
                    }
                    +"Name${getOrderByFieldOrder(SortField.NAME)}"
                }
                Button {
                    attrs {
                        icon = curAspectNameSortModeIcon(SortField.SUBJECT)
                        onClick = { onClickSort(SortField.SUBJECT) }
                    }
                    +"Subject${getOrderByFieldOrder(SortField.SUBJECT)}"
                }
            }
        }
    }

    private fun onClickSort(field: SortField) {
        setState {
            val curOrder = orderBy.find { it.name == field }
            if (curOrder == null)
                orderBy.add(SortOrder(field, Direction.ASC))
            else {
                if (curOrder.direction == Direction.ASC) {
                    orderBy.add(SortOrder(field, Direction.DESC))
                }
                orderBy.remove(curOrder)
            }
            props.onOrderByChanged(orderBy)
        }
    }

    private fun getOrderByFieldOrder(field: SortField) =
        state.orderBy.indexOfFirst { it.name == field }.let { if (it == -1) "" else " (${it + 1})" }

    private fun curAspectNameSortModeIcon(field: SortField): String {
        val directToIcon = mapOf(Direction.ASC to "sort-alphabetical", Direction.DESC to "sort-alphabetical-desc")
        return state.orderBy.find { it.name == field }?.let { directToIcon[it.direction] ?: "double-caret-vertical" }
                ?: "double-caret-vertical"
    }

    interface Props : RProps {
        var onOrderByChanged: (List<SortOrder>) -> Unit
    }

    interface State : RState {
        var orderBy: MutableList<SortOrder>
    }
}


fun RBuilder.aspectSort(block: RHandler<AspectSort.Props>) = child(AspectSort::class, block)