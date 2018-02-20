package catalog.aspects

import catalog.layout.Header
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.h1
import wrappers.RouteSuppliedProps

private val aspectData = arrayOf(
        Aspect("0", "name", "measureUnit", "domain", "baseType"),
        Aspect("1", "name", "measureUnit", "domain", "baseType"),
        Aspect("2", "name", "measureUnit", "domain", "baseType"),
        Aspect("3", "name", "measureUnit", "domain", "baseType"),
        Aspect("4", "name", "measureUnit", "domain", "baseType"),
        Aspect("5", "name", "measureUnit", "domain", "baseType"),
        Aspect("6", "name", "measureUnit", "domain", "baseType"),
        Aspect("7", "name", "measureUnit", "domain", "baseType"),
        Aspect("8", "name", "measureUnit", "domain", "baseType"),
        Aspect("9", "name", "measureUnit", "domain", "baseType"),
        Aspect("10", "name", "measureUnit", "domain", "baseType"),
        Aspect("11", "name", "measureUnit", "domain", "baseType")
)

class AspectsPage : RComponent<RouteSuppliedProps, RState>() {

    override fun RBuilder.render() {
        child(Header::class) {
            attrs { location = props.location.pathname }
        }
        h1 { +"AspectsPage" }
        child(AspectsTable::class) {
            attrs {
                data = aspectData
            }
        }
    }
}
