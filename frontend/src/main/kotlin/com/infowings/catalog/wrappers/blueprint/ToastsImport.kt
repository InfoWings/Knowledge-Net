@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import react.RClass
import react.RProps
import react.ReactElement

external val Toast: RClass<ToastProps>

external interface ToastProps : RProps {
    //var action: ActionProps
    var className: String
    var icon: String // IconName || ReactElement
    var intent: Intent
    var message: ReactElement
    var onDismiss: (didTimeoutExpire: Boolean) -> Unit
    var timeout: Int
}

external val Toaster: RClass<ToasterProps>

external interface ToasterProps : RProps {
    var autoFocus: Boolean
    var canEscapeKeyClear: Boolean
    var className: String
    var position: Position
    var usePortal: Boolean
}
