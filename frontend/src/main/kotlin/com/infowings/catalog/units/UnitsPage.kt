package com.infowings.catalog.units

import com.infowings.catalog.common.MeasureGroupMap
import com.infowings.catalog.layout.Header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.h1

class UnitsPage : RComponent<RouteSuppliedProps, RState>() {
    override fun RBuilder.render() {
        child(Header::class) {
            attrs { location = props.location.pathname }
        }

        h1 { +"Units Page" }

        child(UnitsTable::class) {
            attrs {
                data = MeasureGroupMap.flatMap { it ->
                    it.value.measureList.map { m -> RowData(it.key, m.name, m.symbol) }
                }.toTypedArray()
            }
        }
    }
}