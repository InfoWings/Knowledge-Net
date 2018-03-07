package com.infowings.catalog

import com.infowings.catalog.app.CatalogAppComponent
import kotlinext.js.requireAll
import react.dom.render
import com.infowings.catalog.wrappers.reactRouter
import kotlinext.js.invoke
import kotlinext.js.require
import kotlin.browser.document

fun main(args: Array<String>) {
    requireAll(require.context("css", true, js("/\\.css$/")))
    require("react-table/react-table.css")
    require("react-select/dist/react-select.css")
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
