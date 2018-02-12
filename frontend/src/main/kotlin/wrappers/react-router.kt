package wrappers

import react.*

@JsModule("react-router-dom")
external val reactRouter: ReactRouter = definedExternally

external class ReactRouter {
    var BrowserRouter: RClass<dynamic>
    var Route: RClass<RouteProps>
    //var IndexRoute: RClass<IndexRouteProps>
    var Switch: RClass<RProps>
    var Link: RClass<LinkProps>
    var NavLink: RClass<LinkProps>
    var Redirect: RClass<RedirectProps>
}

class RedirectProps(var to: String) : RProps
class LinkProps(var to: String, var onClick: dynamic = {}) : RProps
class RouterProps(var history: dynamic) : RProps

class RouteProps(var path: String? = null,
                 var component: dynamic,
                 var exact: Boolean = false,
                 var render: dynamic = null) : RProps

class IndexRouteProps(var component: dynamic) : RProps

//class RouterComponent(props: RouterProps) : RComponent<RouterProps, RState>(props) {
//    override fun RBuilder.render() {
//        reactRouter.Router {
//            attrs {
//                this.history = props.history
//            }
//        }
//    }
//}
//
//fun RBuilder.router(browserHistory: dynamic) = child(RouterComponent::class) {
//    attrs {
//        this.history = browserHistory
//    }
//}

class RouteComponent(props: RouteProps, handler: RBuilder.() -> Unit) : RComponent<RouteProps, RState>(props) {
    override fun RBuilder.render() {
        reactRouter.Route {
            attrs {
                this.component = props.component
                this.path = props.path
            }

        }
    }
}

fun RBuilder.route(component: RClass<dynamic>, path: String) = child(RouteComponent::class) {
    attrs {
        this.path = path
        this.component = component
    }
}

//class IndexRouteComponent(props: IndexRouteProps) : RComponent<IndexRouteProps, RState>(props) {
//    override fun RBuilder.render() {
//        reactRouter.IndexRoute {
//            attrs {
//                this.component = props.component
//            }
//        }
//    }
//}
//
//fun RBuilder.indexRoute(component: RClass<dynamic>) = child(IndexRouteComponent::class) {
//    attrs {
//        this.component = component
//    }
//}