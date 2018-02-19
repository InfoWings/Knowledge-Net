package com.infowings.common.catalog.auth

import com.infowings.common.catalog.wrappers.RouteSuppliedProps
import com.infowings.common.catalog.wrappers.reactRouter
import org.w3c.dom.get
import react.RBuilder
import react.buildElements
import kotlin.browser.localStorage

fun RBuilder.privateRoute(path: String, renderFunction: RBuilder.(props: RouteSuppliedProps) -> Unit) {
    reactRouter.Route {
        attrs {
            this.path = path
            this.exact = true
            this.render = { props: RouteSuppliedProps ->
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