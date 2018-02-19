package com.infowings.common.catalog.units

import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.*

class UnitsProps(var units: Map<String, List<String>>) : RProps

class UnitsTable : RComponent<UnitsProps, RState>() {

    override fun RBuilder.render() {
        div("Units-table") {
            table {
                tr {
                    th(classes = "Units-cell Units-header") { +"Value" }
                    th(classes = "Units-cell Units-header") { +"Unit" }
                }
                props.units.map { (valueName, valueUnits) ->
                    tr("Units-row") {
                        td("Units-cell") { b { +valueName } }
                        td("Units-cell") {}
                    }
                    valueUnits.map {
                        tr("Units-row") {
                            td("Units-cell") {}
                            td("Units-cell") { +it }
                        }
                    }
                }
            }
        }
    }
}