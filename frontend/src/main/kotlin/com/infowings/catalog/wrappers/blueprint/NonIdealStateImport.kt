@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import react.RClass
import react.RProps
import react.ReactElement

external val NonIdealState: RClass<NonIdealStateProps>

external interface NonIdealStateProps : RProps {
    var action: ReactElement
    var className: String
    var description: ReactElement
    var title: ReactElement
    var visual: String // IconName || JSXElement
}