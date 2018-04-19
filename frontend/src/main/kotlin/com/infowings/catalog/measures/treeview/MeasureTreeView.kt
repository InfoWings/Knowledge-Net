package com.infowings.catalog.measures.treeview

import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.measures.MeasureGroupData
import react.*
import react.dom.div

class MeasureTreeView(props: MeasureTreeView.Props) : RComponent<MeasureTreeView.Props, RState>(props) {

    override fun RBuilder.render() {
        div(classes = "measures-list") {
            props.groups.mapIndexed { index, (name, description, units) ->
                treeNode {
                    attrs {
                        key = name + index.toString()
                        className = "measure-list--measure"
                        treeNodeContent = buildElement {
                            measureRootLabel {
                                attrs {
                                    groupName = name
                                    groupDescription = description
                                }
                            }
                        }!!
                    }
                    if (units.isNotEmpty()) {
                        measureTreeUnits {
                            attrs {
                                this.units = units
                            }
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var groups: List<MeasureGroupData>
    }
}

fun RBuilder.measureTreeView(block: RHandler<MeasureTreeView.Props>) {
    child(MeasureTreeView::class, block)
}