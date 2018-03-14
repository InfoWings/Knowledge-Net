package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.div

class AspectPropertyEditConsole : RComponent<AspectPropertyEditConsole.Props, RState>() {

    private fun handleKeyDown(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        val keyCode = e.unsafeCast<KeyboardEvent>().keyCode
        when (keyCode) {
            27 -> props.onCancel()
            13 -> console.log("Submit")
        }
    }

    override fun RBuilder.render() {
        div(classes = "aspect-edit-console") {}
    }

    interface Props : RProps {
        var aspectProperty: AspectPropertyData
        var onCancel: () -> Unit
        var onSubmit: (AspectData) -> Unit
    }

    interface State : RState {

    }
}

fun RBuilder.aspectPropertyEditConsole(block: RHandler<AspectPropertyEditConsole.Props>) = child(AspectPropertyEditConsole::class, block)