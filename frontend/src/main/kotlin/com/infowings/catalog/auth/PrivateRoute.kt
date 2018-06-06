package com.infowings.catalog.auth

import com.infowings.catalog.utils.getAuthorizationRole
import com.infowings.catalog.wrappers.RouteSuppliedProps
import com.infowings.catalog.wrappers.reactRouter
import react.RBuilder
import react.buildElements

fun RBuilder.privateRoute(path: String, exact: Boolean = true, renderFunction: RBuilder.(props: RouteSuppliedProps) -> Unit) {
    reactRouter.Route {
        attrs {
            this.path = path
            this.exact = exact
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