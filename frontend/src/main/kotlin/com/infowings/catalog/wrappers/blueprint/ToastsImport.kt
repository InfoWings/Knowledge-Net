@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import org.w3c.dom.HTMLElement
import react.*

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

external class Toaster : Component<ToasterProps, RState> {
    override fun render(): dynamic

    companion object {
        fun create(toasterProps: ToasterProps, container: HTMLElement = definedExternally): StaticToaster
    }
}

external interface ToasterProps : RProps {
    var autoFocus: Boolean
    var canEscapeKeyClear: Boolean
    var className: String
    var position: Position
    var usePortal: Boolean
}

external interface StaticToaster {
    fun clear()
    fun dismiss(key: String)
    fun getToasts(): Array<ToastProps>
    fun show(toast: ToastProps, key: String?): String
}
