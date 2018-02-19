package com.infowings.common.catalog

import com.infowings.common.catalog.app.CatalogAppComponent
import kotlinext.js.requireAll
import react.dom.render
import wrappers.reactRouter
import kotlin.browser.document

fun main(args: Array<String>) {
    requireAll(kotlinext.js.require.context("css", true, js("/\\.css$/")))
    render(document.getElementById("root")) {
        reactRouter.BrowserRouter {
            reactRouter.Route {
                attrs {
                    path = "/"
                    component = ::CatalogAppComponent
                }
            }
        }
    }
}
