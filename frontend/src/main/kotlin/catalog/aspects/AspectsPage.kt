package catalog.aspects

import catalog.layout.Header
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.h1
import wrappers.RouteSuppliedProps

private val aspectData = arrayOf(
        AspectData("0", "name", "measureUnit", "domain", "baseType", listOf(
                AspectPropertyData("0", "name", "1", "power"),
                AspectPropertyData("1", "name", "2", "power"),
                AspectPropertyData("2", "name", "11", "power")
        )),
        AspectData("1", "name", "measureUnit", "domain", "baseType", listOf(
                AspectPropertyData("3", "name", "11", "power")
        )),
        AspectData("2", "name", "measureUnit", "domain", "baseType", listOf(
                AspectPropertyData("4", "name", "9", "power"),
                AspectPropertyData("5", "name", "5", "power")
        )),
        AspectData("3", "name", "measureUnit", "domain", "baseType", listOf(
                AspectPropertyData("6", "name", "7", "power"),
                AspectPropertyData("7", "name", "9", "power")
        )),
        AspectData("4", "name", "measureUnit", "domain", "baseType", listOf(
                AspectPropertyData("8", "name", "10", "power"),
                AspectPropertyData("9", "name", "11", "power")
        )),
        AspectData("5", "name", "measureUnit", "domain", "baseType", listOf(
                AspectPropertyData("10", "name", "8", "power"),
                AspectPropertyData("11", "name", "9", "power")
        )),
        AspectData("6", "name", "measureUnit", "domain", "baseType", listOf(
                AspectPropertyData("12", "name", "11", "power")
        )),
        AspectData("7", "name", "measureUnit", "domain", "baseType"),
        AspectData("8", "name", "measureUnit", "domain", "baseType"),
        AspectData("9", "name", "measureUnit", "domain", "baseType"),
        AspectData("10", "name", "measureUnit", "domain", "baseType"),
        AspectData("11", "name", "measureUnit", "domain", "baseType")
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
                aspectsMap = aspectData.associate { aspect -> Pair(aspect.id, aspect) }
            }
        }
    }
}
