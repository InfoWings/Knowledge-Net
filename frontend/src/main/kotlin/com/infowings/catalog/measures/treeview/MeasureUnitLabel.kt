package com.infowings.catalog.measures.treeview

import com.infowings.catalog.measures.UnitData
import react.*
import react.dom.div
import react.dom.span

class MeasureUnitLabel(props: MeasureUnitLabel.Props) : RComponent<MeasureUnitLabel.Props, RState>(props) {
    override fun RBuilder.render() {
        val containsFilterText = if (props.unit.containsFilterText) "" else "not_contains_filter_text"
        div(classes = "measures-list--measure $containsFilterText") {
            span(classes = "text-bold text-italic") {
                +props.unit.name
            }
            +":"
            span(classes = "text-grey") {
                +props.unit.symbol
            }
        }
    }

    interface Props : RProps {
        var unit: UnitData
    }
}

fun RBuilder.measureUnitLabel(block: RHandler<MeasureUnitLabel.Props>) {
    child(MeasureUnitLabel::class, block)
}
