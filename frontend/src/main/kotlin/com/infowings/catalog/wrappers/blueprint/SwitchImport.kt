@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.RClass

external val Switch: RClass<SwitchProps>

external interface SwitchProps : BlueprintComponentProps {
    var alignIndicator: Alignment
    var checked: Boolean
    var defaultChecked: Boolean
    var disabled: Boolean
    var inline: Boolean
    var label: String
    var inputRef: (HTMLInputElement?) -> Unit
    var large: Boolean
    var onChange: (Event) -> Unit
}
