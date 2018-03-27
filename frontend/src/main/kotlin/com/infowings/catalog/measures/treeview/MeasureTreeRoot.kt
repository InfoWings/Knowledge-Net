package com.infowings.catalog.measures.treeview

import com.infowings.catalog.measures.UnitData
import com.infowings.catalog.utils.squareMinusIcon
import com.infowings.catalog.utils.squarePlusIcon
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.svg

class MeasureTreeRoot(props: MeasureTreeRoot.Props) : RComponent<MeasureTreeRoot.Props, MeasureTreeRoot.State>(props) {
    private fun handleExpanderClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        setState {
            expanded = !expanded
        }
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view--root") {
            if (props.units.isNotEmpty()) {
                if (state.expanded) {
                    squareMinusIcon(classes = "aspect-tree-view--line-icon aspect-tree-view--line-icon__clickable") {
                        attrs.onClickFunction = ::handleExpanderClick
                    }
                } else {
                    squarePlusIcon(classes = "aspect-tree-view--line-icon aspect-tree-view--line-icon__clickable") {
                        attrs.onClickFunction = ::handleExpanderClick
                    }
                }
            } else {
                svg(classes = "aspect-tree-view--line-icon")
            }
            child(MeasureRootLabel::class) {
                attrs {
                    groupName = props.groupName
                }
            }
        }
        if (props.units.isNotEmpty() && state.expanded) {
            child(MeasureTreeUnits::class) {
                attrs {
                    units = props.units
                }
            }
        }
    }

    interface Props : RProps {
        var groupName: String
        var units: List<UnitData>
    }

    interface State : RState {
        var expanded: Boolean
    }
}