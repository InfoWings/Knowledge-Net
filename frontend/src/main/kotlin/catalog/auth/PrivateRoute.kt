package catalog.auth

import org.w3c.dom.get
import react.RBuilder
import react.RProps
import react.buildElements
import wrappers.reactRouter
import kotlin.browser.localStorage

class PrivateRouteProps(var component: RBuilder.(props: RProps) -> Unit, var path: String, var exact: Boolean = false) : RProps

fun RBuilder.privateRoute(path: String, exact: Boolean = false, renderFunction: RBuilder.(props: RProps) -> Unit) {
    reactRouter.Route {
        attrs {
            this.path = path
            this.render = { props: RProps ->
                if (localStorage["auth-role"] != null) {
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