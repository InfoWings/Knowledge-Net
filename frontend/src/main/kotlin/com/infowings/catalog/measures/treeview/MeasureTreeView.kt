package com.infowings.catalog.measures.treeview

import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.measures.MeasureGroupData
import kotlinext.js.invoke
import kotlinext.js.require
import react.*
import react.dom.div

class MeasureTreeView(props: MeasureTreeView.Props) : RComponent<MeasureTreeView.Props, RState>(props) {
    companion object {
        init {
            require("styles/measures-list.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "measures-list") {
            props.groups.mapIndexed { index, (name, units) ->
                treeNode {
                    attrs {
                        key = name + index.toString()
                        className = "aspect-tree-view--aspect-node"
                        treeNodeContent = buildElement {
                            measureRootLabel {
                                attrs {
                                    groupName = name
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