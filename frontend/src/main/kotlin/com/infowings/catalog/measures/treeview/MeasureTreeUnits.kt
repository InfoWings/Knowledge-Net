package com.infowings.catalog.measures.treeview

import com.infowings.catalog.measures.UnitData
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.div

class MeasureTreeUnits(props: MeasureTreeUnits.Props) : RComponent<MeasureTreeUnits.Props, RState>(props) {
    override fun RBuilder.render() {
        div(classes = "aspect-tree-view--properties-block") {
            props.units.map { unit ->
                child(MeasureTreeUnit::class) {
                    attrs {
                        this.unit = unit
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var units: List<UnitData>
    }
}
