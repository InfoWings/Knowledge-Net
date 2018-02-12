//package index
//
//import kotlinext.js.require
//import kotlinext.js.requireAll
//import react.dom.render
//import catalog.test.*
//import wrappers.*
//import kotlin.browser.document
//
//fun main(args: Array<String>) {
//    requireAll(require.context(".", true, js("/\\.css$/")))
////    val appReducer = combineReducers(toPlainObjectStripNull(Reducers))
////    val store = createStore(appReducer, applyMiddleware(thunk.default))
////    render(document.getElementById("root")) {
////        Provider {
////            attrs.store = store
////            testApp()
////            //app()
////        }
////    }
//
//    render(document.getElementById("root")) {
//        console.log(reactRouter)
//        reactRouter.BrowserRouter {
//            reactRouter.Switch {
//                reactRouter.Route {
//                    attrs {
//                        path = "/"
//                        component = ::TestAppComponent
//                    }
//                }
//            }
//        }
//    }
//}
//
//
///**
// *
// *
// * import { createStore, applyMiddleware, combineReducers } from 'redux'
//import { Reducers as gridReducers } from 'react-redux-grid';
//
//const rootReducer = combineReducers({
//...gridReducers,
//});
//
//export function configureStore() {
//return createStore(
//rootReducer,
//applyMiddleware(
//thunk
//)
//)
//}
//
// */