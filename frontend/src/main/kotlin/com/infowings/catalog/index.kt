package com.infowings.catalog

import com.infowings.catalog.app.CatalogAppComponent
import kotlinext.js.requireAll
import react.dom.render
import com.infowings.catalog.wrappers.reactRouter
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
