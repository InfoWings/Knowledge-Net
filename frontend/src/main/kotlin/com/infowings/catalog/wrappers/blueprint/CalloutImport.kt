@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import react.RClass

external val Callout: RClass<CalloutProps>

external interface CalloutProps : BlueprintComponentProps {
    var icon: String
    var intent: Intent
    var title: String
}