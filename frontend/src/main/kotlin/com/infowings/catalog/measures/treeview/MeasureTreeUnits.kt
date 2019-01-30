package com.infowings.catalog.measures.treeview

import com.infowings.catalog.measures.UnitData
import react.*
import react.dom.li
import react.dom.ul

class MeasureTreeUnits(props: MeasureTreeUnits.Props) : RComponent<MeasureTreeUnits.Props, RState>(props) {
    override fun RBuilder.render() {
        ul(classes = "bp3-list-unstyled") {
            props.units.map { unit ->
                li(classes = "unit-row") {
                    attrs {
                        key = unit.name
                        measureTreeUnit {
                            attrs {
                                this.unit = unit
                            }
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var units: List<UnitData>
    }
}


fun RBuilder.measureTreeUnits(block: RHandler<MeasureTreeUnits.Props>) {
    child(MeasureTreeUnits::class, block)
}