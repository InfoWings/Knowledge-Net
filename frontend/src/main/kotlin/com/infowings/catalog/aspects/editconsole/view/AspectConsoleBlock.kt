package com.infowings.catalog.aspects.editconsole.view

import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.div

/**
 * View component. Serves as wrapper to console that handles some keystrokes for flow.
 */
class AspectConsoleBlock : RComponent<AspectConsoleBlock.Props, RState>() {

    private fun handleKeyDown(e: Event) {
        e.stopPropagation()
        val keyCode = e.unsafeCast<KeyboardEvent>().keyCode
        val ctrlPressed = e.unsafeCast<KeyboardEvent>().ctrlKey
        when (keyCode) {
            27 -> props.onEscape()
            13 -> if (ctrlPressed) props.onCtrlEnter() else props.onEnter()
        }
    }

    override fun RBuilder.render() {
        div(classes = "aspect-edit-console") {
            attrs.onKeyDownFunction = ::handleKeyDown
            children()
        }
    }

    interface Props : RProps {
        var onEnter: () -> Unit
        var onCtrlEnter: () -> Unit
        var onEscape: () -> Unit
    }
}

fun RBuilder.aspectConsoleBlock(block: RHandler<AspectConsoleBlock.Props>) = child(AspectConsoleBlock::class, block)