@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import react.RClass
import react.RProps

external val Overlay: RClass<IOverlayProps>

external interface IOverlayProps : RProps {
    var autoFocus: Boolean
    var backdropClassName: String
    //var backdropProps: dynamic
    var canEscapeKeyClose: Boolean
    var canOutsideClickClose: Boolean
    var className: String
    var didClose: (HTMLElement) -> Unit
    var didOpen: () -> Unit
    var enforceFocus: Boolean
    var hasBackdrop: Boolean
    var isOpen: Boolean
    var lazy: Boolean
    var onClose: (event: Event) -> Unit
    var transitionDuration: Int
    var transitionName: String
    var usePortal: Boolean
}