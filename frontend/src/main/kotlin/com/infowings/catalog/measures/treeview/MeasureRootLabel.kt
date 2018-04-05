package com.infowings.catalog.measures.treeview

import react.*
import react.dom.div
import react.dom.span

class MeasureRootLabel(props: MeasureRootLabel.Props) : RComponent<MeasureRootLabel.Props, RState>(props) {
    override fun RBuilder.render() {
        div(classes = "measures-list--measure-group") {
            span(classes = "text-bold") {
                +props.groupName
            }
        }
    }

    interface Props : RProps {
        var groupName: String
    }
}

fun RBuilder.measureRootLabel(block: RHandler<MeasureRootLabel.Props>) {
    child(MeasureRootLabel::class, block)
}