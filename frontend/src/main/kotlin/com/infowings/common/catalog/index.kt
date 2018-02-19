package com.infowings.common.catalog

import com.infowings.common.catalog.app.CatalogAppComponent
import com.infowings.common.catalog.wrappers.reactRouter
import kotlinext.js.require
import kotlinext.js.require.context
import kotlinext.js.requireAll
import react.dom.render
import kotlin.browser.document

fun main(args: Array<String>) {
    requireAll(context("css", true, js("/\\.css$/")))
    require.resolve("react-table/react-table.css")
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
