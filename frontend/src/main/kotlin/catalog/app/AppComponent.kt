package catalog.app

import catalog.auth.AuthComponent
import catalog.auth.privateRoute
import catalog.home.HomeComponent
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import wrappers.reactRouter

class CatalogAppComponent : RComponent<RProps, RState>() {

    override fun RBuilder.render() {
        reactRouter.Switch {
            reactRouter.Route {
                attrs {
                    path = "/login"
                    component = ::AuthComponent
                }
            }
            privateRoute("/home", renderFunction = { child(HomeComponent::class) {} })
            reactRouter.Route {
                attrs {
                    path = "/"
                    exact = true
                    render = { reactRouter.Redirect { attrs.to = "/home" } }
                }
            }
        }
    }
}