@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import react.RClass
import react.RProps
import react.ReactElement

external val Collapse: RClass<CollapseProps>

external interface CollapseProps : RProps {
    var className: String
    var component: ReactElement
    var isOpen: Boolean
    var keepChildrenMounted: Boolean
    var transitionDuration: Int
}