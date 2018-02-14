package catalog.measurements

import catalog.layout.Header
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.h1
import wrappers.RouteSuppliedProps

class MeasurementsPage : RComponent<RouteSuppliedProps, RState>() {

    override fun RBuilder.render() {
        child(Header::class) {
            attrs { location = props.location.pathname }
        }
        h1 { +"Measures Page" }
    }
}