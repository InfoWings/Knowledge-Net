package com.infowings.catalog.objects.view.sort

import com.infowings.catalog.common.SortOrder
import com.infowings.catalog.common.SortField
import com.infowings.catalog.common.Direction
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.ButtonGroup
import kotlinext.js.require
import react.*
import react.dom.div

class ObjectSort : RComponent<ObjectSort.Props, ObjectSort.State>() {

    companion object {
        init {
            require("styles/aspect-sort.scss")
        }

        val sortPrio = mapOf(SortField.NAME to 0, SortField.SUBJECT to 1)
    }

    override fun ObjectSort.State.init() {
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
        println("ON CLICK: field ${field.name} ${field.ordinal} ${field}")
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


fun RBuilder.objectSort(block: RHandler<ObjectSort.Props>) = child(ObjectSort::class, block)
