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
    // Custom styles
    requireAll(context("styles", true, js("/\\.css$/").unsafeCast<RegExp>()))
    require("styles/main.scss")
    // Styles for react-table component
    require("react-table/react-table.css")
    // Styles for react-select component
    require("react-select/dist/react-select.css")
    // Styles for blueprintjs components
    require("@blueprintjs/core/lib/css/blueprint.css")
    require("@blueprintjs/icons/lib/css/blueprint-icons.css")
    require("normalize.css/normalize.css")

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
