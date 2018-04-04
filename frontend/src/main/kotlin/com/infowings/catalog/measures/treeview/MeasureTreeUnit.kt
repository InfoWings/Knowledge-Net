package com.infowings.catalog.measures.treeview

import com.infowings.catalog.measures.UnitData
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState

class MeasureTreeUnit(props: MeasureTreeUnit.Props) : RComponent<MeasureTreeUnit.Props, RState>(props) {
    override fun RBuilder.render() {
        child(MeasureUnitLabel::class) {
            attrs {
                unit = props.unit
            }
        }
    }

    interface Props : RProps {
        var unit: UnitData
    }
}