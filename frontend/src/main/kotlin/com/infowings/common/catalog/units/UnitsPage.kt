package com.infowings.common.catalog.units

import com.infowings.common.catalog.layout.Header
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.h1
import wrappers.RouteSuppliedProps

class UnitsPage : RComponent<RouteSuppliedProps, RState>() {
    val data = mapOf(
        "Length" to listOf("Metre", "Cantimetre", "Millimetre", "Inch", "Mile"),
        "Mass" to listOf("Kilogram", "Milligram", "Pound"),
        "Time" to listOf("Second", "Millisecond", "Minute", "Hour"),
        "Speed" to listOf("KilometrePerSecond", "MilePerHour", "InchPerSecond", "MetrePerSecond", "KilometrePerHour")
    )

    override fun RBuilder.render() {
        child(Header::class) {
            attrs { location = props.location.pathname }
        }

        h1 { +"Units Page" }

        child(UnitsTable::class) {
            attrs {
                units = data
            }
        }
    }
}