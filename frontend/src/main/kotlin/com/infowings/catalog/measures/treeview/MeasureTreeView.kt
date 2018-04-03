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
            require("styles/aspect-tree-view.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view") {
            props.groups.mapIndexed { index, group ->
                treeNode {
                    attrs {
                        key = group.name + index.toString()
                        className = "aspect-tree-view--aspect-node"
                        treeNodeContent = buildElement {
                            child(MeasureRootLabel::class) {
                                attrs {
                                    groupName = group.name
                                }
                            }
                        }!!
                    }
                    if (group.units.isNotEmpty()) {
                        child(MeasureTreeUnits::class) {
                            attrs {
                                units = group.units
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