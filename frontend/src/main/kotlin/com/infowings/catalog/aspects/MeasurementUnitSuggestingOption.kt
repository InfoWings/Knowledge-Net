package com.infowings.catalog.aspects

import com.infowings.catalog.wrappers.select.OptionComponentProps
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onMouseMoveFunction
import kotlinx.html.role
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.div


class MeasurementUnitSuggestingOption : RComponent<OptionComponentProps<MeasurementUnitOption>, RState>() {

    private var optionRef: HTMLDivElement? = null

    private fun handleClick(event: Event) {
        event.preventDefault()
        event.stopPropagation()
        props.onSelect(props.option, event)
    }

    private fun handleMouseMove(event: Event) {
        onFocus(event)
    }

    private fun onFocus(event: Event) {
        if (!props.isFocused) {
            props.onFocus(props.option, event)
        }
    }

    override fun RBuilder.render() {
        div(classes = "mu-option ${props.className}") {
            attrs {
                role = "option"
                onClickFunction = ::handleClick
                onMouseMoveFunction = ::handleMouseMove
                ref { ref -> optionRef = ref as HTMLDivElement? }
            }
            if (props.isFocused) {
                child(MeasurementUnitOptionCategoryContainer::class) {
                    attrs {
                        measurementUnit = props.option.measurementUnit
                        onUnitClick = { optionName, event -> props.onSelect(measurementUnitOption(optionName), event) }
                        relativeHeight = optionRef?.offsetTop
                    }
                }
            }
            children()
        }
    }

}