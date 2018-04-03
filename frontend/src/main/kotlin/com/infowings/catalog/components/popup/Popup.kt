package com.infowings.catalog.components.popup

import kotlinx.html.js.onClickFunction
import react.*
import react.dom.div

class Popup : RComponent<Popup.Props, RState>() {

    override fun RBuilder.render() {
        div(classes = "popup_outer") {
            attrs {
                onClickFunction = { props.closePopup() }
            }
        }
        div(classes = "popup") {
            children()
        }
    }

    interface Props : RProps {
        var closePopup: () -> Unit
    }
}

fun RBuilder.popup(block: RHandler<Popup.Props>) = child(Popup::class, block)