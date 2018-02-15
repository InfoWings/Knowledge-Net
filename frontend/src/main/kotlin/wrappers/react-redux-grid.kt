@file:JsModule("react-redux-grid")

package wrappers

import catalog.aspects.GridProps
import react.RClass

external val Reducers: dynamic = definedExternally
external val Grid: RClass<GridProps> = definedExternally

//fun GridFactory( handler: T.() -> Unit ){
//    Grid{
//        attrs{
//            handler.invoke()
//        }
//    }
//}