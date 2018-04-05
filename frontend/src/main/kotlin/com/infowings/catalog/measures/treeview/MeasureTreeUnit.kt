package com.infowings.catalog.measures.treeview

import com.infowings.catalog.measures.UnitData
import react.*

class MeasureTreeUnit(props: MeasureTreeUnit.Props) : RComponent<MeasureTreeUnit.Props, RState>(props) {
    override fun RBuilder.render() {
        measureUnitLabel {
            attrs {
                unit = props.unit
            }
        }
    }

    interface Props : RProps {
        var unit: UnitData
    }
}

fun RBuilder.measureTreeUnit(block: RHandler<MeasureTreeUnit.Props>) {
    child(MeasureTreeUnit::class, block)
}