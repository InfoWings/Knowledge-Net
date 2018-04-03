package com.infowings.catalog.measures.treeview

import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.measures.UnitData
import react.*
import react.dom.div

class MeasureTreeUnits(props: MeasureTreeUnits.Props) : RComponent<MeasureTreeUnits.Props, RState>(props) {
    override fun RBuilder.render() {
        div(classes = "aspect-tree-view--aspect-node") {
            props.units.map { unit ->
                treeNode {
                    attrs {
                        treeNodeContent = buildElement {
                            child(MeasureTreeUnit::class) {
                                attrs {
                                    this.unit = unit
                                }
                            }
                        }!!
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var units: List<UnitData>
    }
}
