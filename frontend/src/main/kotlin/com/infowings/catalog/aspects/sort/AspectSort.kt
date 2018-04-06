package com.infowings.catalog.aspects.sort

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectOrderBy
import com.infowings.catalog.common.AspectSortField
import com.infowings.catalog.common.Direction
import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Label
import react.*


/**
 * View Component. Draws List of [treeNode] for each [AspectData] in [AspectTreeView.Props.aspects] list
 */
class AspectSort : RComponent<AspectSort.Props, AspectSort.State>() {

    override fun AspectSort.State.init() {
        orderBy = mutableListOf(AspectOrderBy(AspectSortField.NAME, Direction.ASC))
    }

    override fun RBuilder.render() {
        Button {
            attrs {
                className = "pt-button ${curAspectNameSortModeIcon(AspectSortField.NAME)}"
                onClick = { onClickSort(AspectSortField.NAME) }
            }
            +"name"
        }
        Button {
            attrs {
                onClick = { onClickSort(AspectSortField.SUBJECT) }
                className = "pt-button ${curAspectNameSortModeIcon(AspectSortField.SUBJECT)}"
            }
            +"subject"
        }
        Label {
            +"Current sort mode: ${printSortMode()}"
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

    private fun curAspectNameSortModeIcon(field: AspectSortField): String {
        val directToIcon = mapOf(Direction.ASC to "pt-icon-sort-asc", Direction.DESC to "pt-icon-sort-desc")
        state.orderBy.find { it.name == field }?.let { return directToIcon[it.direction] ?: "" }
        return ""
    }

    private fun printSortMode() =
        state.orderBy.joinToString(separator = ", ") { it.name.toString() + " " + it.direction }

    interface Props : RProps {
        var onFetchAspect: (List<AspectOrderBy>) -> Unit
    }

    interface State : RState {
        var orderBy: MutableList<AspectOrderBy>
    }
}


fun RBuilder.aspectSort(block: RHandler<AspectSort.Props>) = child(AspectSort::class, block)