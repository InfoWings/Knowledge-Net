@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import react.RClass
import react.RProps
import react.ReactElement

external interface BlueprintComponentProps : RProps {
    var className: String
}

external val Navbar: RClass<NavbarProps>

external interface NavbarProps : BlueprintComponentProps

external val NavbarGroup: RClass<NavbarGroupProps>

external interface NavbarGroupProps : BlueprintComponentProps {
    var align: Alignment
}

external val NavbarHeading: RClass<NavbarHeadingProps>

external interface NavbarHeadingProps : BlueprintComponentProps

external val NavbarDivider: RClass<NavbarDividerProps>

external interface NavbarDividerProps : BlueprintComponentProps

external val Button: RClass<ButtonProps>

external interface ButtonProps : BlueprintComponentProps {
    var active: Boolean
    var alignText: Alignment
    var disabled: Boolean
    var elementRef: (ref: HTMLElement?) -> Unit
    var icon: String // TODO: Or ReactElement, decide later
    var intent: Intent
    var loading: Boolean
    var onClick: (MouseEvent) -> Unit
    var rightIcon: String // TODO: Or ReactElement, decide later
    var text: ReactElement? // TODO: Or just String, decide later
    var type: String
}

external val Label: RClass<LabelProps>

external interface LabelProps : BlueprintComponentProps {
    var disabled: Boolean
    var helperText: ReactElement? // TODO: Or just String
    var text: ReactElement? // TODO: Or just String
}
