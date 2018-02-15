package catalog

import catalog.app.CatalogAppComponent
import kotlinext.js.requireAll
import kotlinext.js.toPlainObjectStripNull
import react.dom.render
import wrappers.*
import kotlin.browser.document

fun main(args: Array<String>) {
    requireAll(kotlinext.js.require.context("css", true, js("/\\.css$/")))
    val appReducer = combineReducers(toPlainObjectStripNull(Reducers))
    val store = createStore(appReducer, applyMiddleware(thunk.default))
    render(document.getElementById("root")) {
        Provider {
            attrs.store = store
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
}
