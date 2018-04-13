@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import org.w3c.dom.events.MouseEvent
import react.RClass
import react.RProps

external val Alert: RClass<IAlertProps>

external interface IAlertProps : RProps {
    var cancelButtonText: String
    var canEscapeKeyCancel: Boolean
    var canOutsideClickCancel: Boolean
    var className: String
    var confirmButtonText: String
    var icon: String
    var intent: Intent
    var isOpen: Boolean
    var onCancel: (MouseEvent) -> Unit
    var onClose: (confirmed: Boolean, evt: MouseEvent) -> Unit
    var onConfirm: (MouseEvent) -> Unit
    var style: dynamic // todo CSSProperties
    var transitionDuration: Int
}