@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import react.RClass
import react.ReactElement

external val Tooltip: RClass<TooltipProps>

external interface TooltipProps : BlueprintComponentProps {
    var content: ReactElement?
    var defaultIsOpen: Boolean
    var disabled: Boolean
    var hoverCloseDelay: Int
    var hoverOpenDelay: Int
    var inheritDarkTheme: Boolean
    var intent: Intent
    var isOpen: Boolean
    // var modifiers: Object // Popper Modifiers documented in https://popper.js.org/popper-documentation.html#modifiers
    var onInteraction: (nextOpenState: Boolean) -> Unit
    var openOnTargetFocus: Boolean
    var portalClassName: String
    var position: Position // Default value ("auto") will chose the best position
    var rootElementTag: String
    var targetElementTag: String
    var tooltipClassName: String
    var transitionDuration: Int
    var usePortal: Boolean
}

