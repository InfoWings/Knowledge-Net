package com.infowings.catalog

import com.infowings.catalog.app.CatalogAppComponent
import com.infowings.catalog.wrappers.reactRouter
import kotlinext.js.invoke
import kotlinext.js.require
import kotlinext.js.require.context
import kotlinext.js.requireAll
import react.dom.render
import kotlin.browser.document
import kotlin.js.RegExp

fun main(args: Array<String>) {
    requireAll(context("styles", true, js("/\\.css$/").unsafeCast<RegExp>()))
    require("styles/main.scss")
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
