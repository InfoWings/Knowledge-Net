package com.infowings.catalog.measures.treeview

import com.infowings.catalog.measures.MeasureGroupData
import kotlinext.js.invoke
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.div

class MeasureTreeView(props: MeasureTreeView.Props) : RComponent<MeasureTreeView.Props, RState>(props) {
    companion object {
        init {
            kotlinext.js.require("styles/aspect-tree-view.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view") {
            props.groups.map { group ->
                child(MeasureTreeRoot::class) {
                    attrs {
                        groupName = group.name
                        units = group.units
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var groups: List<MeasureGroupData>
    }
}