package com.infowings.catalog.measures.treeview

import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.div
import react.dom.span

class MeasureRootLabel(props: MeasureRootLabel.Props) : RComponent<MeasureRootLabel.Props, RState>(props) {
    override fun RBuilder.render() {
        div(classes = "aspect-tree-view--label") {
            span(classes = "aspect-tree-view--label-name") {
                +props.groupName
            }
        }
    }

    interface Props : RProps {
        var groupName: String
    }
}