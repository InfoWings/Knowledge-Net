package com.infowings.catalog.measures.treeview

import com.infowings.catalog.components.description.descriptionComponent
import react.*
import react.dom.div
import react.dom.span

class MeasureRootLabel(props: MeasureRootLabel.Props) : RComponent<MeasureRootLabel.Props, RState>(props) {
    override fun RBuilder.render() {
        div(classes = "measures-list--measure-group") {
            span(classes = "text-bold") {
                +props.groupName
            }
            descriptionComponent(
                className = "measures-list--description-icon",
                description = props.groupDescription
            )
        }
    }

    interface Props : RProps {
        var groupName: String
        var groupDescription: String?
    }
}

fun RBuilder.measureRootLabel(block: RHandler<MeasureRootLabel.Props>) {
    child(MeasureRootLabel::class, block)
}