package com.infowings.catalog.aspects.search

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.div
import react.dom.input
import react.dom.span

class AspectSearchComponent : RComponent<AspectSearchComponent.Props, AspectSearchComponent.State>() {

    companion object {
        init {
            kotlinext.js.require("styles/aspect-search.scss")
        }
    }

    override fun State.init() {
        searchQuery = ""
    }

    private fun handleChange(event: Event) {
        val query = event.target.unsafeCast<HTMLInputElement>().value
        setState {
            searchQuery = query
        }
    }

    private fun handleKeyDown(event: Event) {
        val keyDownEvent = event.unsafeCast<KeyboardEvent>()
        if (keyDownEvent.keyCode == 13) {
            props.onConfirmSearch(state.searchQuery)
        }
    }

    override fun RBuilder.render() {
        div(classes = "bp3-input-group aspect-search") {
            span(classes = "bp3-icon bp3-icon-search") {}
            input(type = InputType.text, classes = "bp3-input") {
                attrs {
                    value = state.searchQuery
                    placeholder = "Search Aspect..."
                    onKeyDownFunction = this@AspectSearchComponent::handleKeyDown
                    onChangeFunction = this@AspectSearchComponent::handleChange
                }
            }
            Button {
                attrs {
                    className = "bp3-minimal"
                    intent = Intent.NONE
                    icon = "delete"
                    onClick = {
                        setState {
                            searchQuery = ""
                        }
                        props.onConfirmSearch("")
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var onConfirmSearch: (String) -> Unit
    }

    interface State : RState {
        var searchQuery: String
    }
}

fun RBuilder.aspectSearchComponent(block: RHandler<AspectSearchComponent.Props>) =
    child(AspectSearchComponent::class, block)

