package com.infowings.catalog.measures.treeview

import com.infowings.catalog.measures.UnitData
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.div
import react.dom.span

class MeasureUnitLabel(props: MeasureUnitLabel.Props) : RComponent<MeasureUnitLabel.Props, RState>(props) {
    override fun RBuilder.render() {
        div(classes = "aspect-tree-view--label") {
            span(classes = "aspect-tree-view--label-property-cardinality") {
                +props.unit.name
            }
            +":"
            span(classes = "aspect-tree-view--label-property-cardinality") {
                +props.unit.symbol
            }
        }
    }

    interface Props : RProps {
        var unit: UnitData
    }
}
