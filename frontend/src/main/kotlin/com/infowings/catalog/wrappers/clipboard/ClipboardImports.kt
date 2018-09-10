package com.infowings.catalog.wrappers.clipboard

import org.w3c.dom.Element
import org.w3c.dom.events.Event

@JsModule("clipboard")
@Suppress("UnusedPrivateMember")
external class ClipboardJS(selector: String, options: ClipboardOptions = definedExternally) {
    fun on(name: String, fn: (Event) -> Unit)
    fun destroy()
}

external interface ClipboardOptions {
    var target: (trigger: Element) -> Element
    var text: (trigger: Element) -> String
    var container: Element
}