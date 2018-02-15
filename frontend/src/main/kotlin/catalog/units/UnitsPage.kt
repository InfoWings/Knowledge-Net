package catalog.units

import catalog.layout.Header
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.*
import wrappers.RouteSuppliedProps

class UnitsPage : RComponent<RouteSuppliedProps, RState>() {

    var units: List<String> = listOf("m", "cm", "mm", "kg")

    override fun RBuilder.render() {
        child(Header::class) {
            attrs { location = props.location.pathname }
        }

        h1 { +"Units Page" }

        div("Units-table") {

            table {
                tr { th { +"Unit" } }
                units.map { tr { td { +it } } }
            }
        }
    }
}