package com.infowings.catalog.measures.treeview

import com.infowings.catalog.measures.UnitData
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.li
import react.dom.ul

class MeasureTreeUnits(props: MeasureTreeUnits.Props) : RComponent<MeasureTreeUnits.Props, RState>(props) {
    override fun RBuilder.render() {
        ul(classes = "pt-list-unstyled") {
            props.units.map { unit ->
                li(classes = "unit-row") {
                    attrs {
                        key = unit.name
                        child(MeasureTreeUnit::class) {
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
