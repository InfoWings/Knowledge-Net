@file:JsModule("@blueprintjs/core")
@file:Suppress("MatchingDeclarationName")

package com.infowings.catalog.wrappers.blueprint

import react.RClass
import react.RProps

external val Spinner: RClass<SpinnerProps>

external interface SpinnerProps : RProps {
    var className: String
    var intent: Intent
    var large: Boolean
    var small: Boolean
    var value: Number
}