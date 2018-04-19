@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import react.RClass
import react.RProps

external val ButtonGroup: RClass<ButtonGroupProps>

external interface ButtonGroupProps : RProps {
    var alignText: Alignment
    var className: String
    var fill: Boolean
    var large: Boolean
    var minimal: Boolean
    var vertical: Boolean
}
