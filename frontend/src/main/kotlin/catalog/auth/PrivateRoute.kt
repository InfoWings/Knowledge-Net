package catalog.auth

import react.RBuilder
import react.buildElements
import utils.getAuthorizationRole
import wrappers.RouteSuppliedProps
import wrappers.reactRouter

fun RBuilder.privateRoute(path: String, renderFunction: RBuilder.(props: RouteSuppliedProps) -> Unit) {
    reactRouter.Route {
        attrs {
            this.path = path
            this.exact = true
            this.render = { props: RouteSuppliedProps ->
                if (getAuthorizationRole() != null) {
                    buildElements {
                        renderFunction(props)
                    }
                } else {
                    reactRouter.Redirect {
                        attrs {
                            to = "/login"
                        }
                    }
                }
            }
        }
    }
}