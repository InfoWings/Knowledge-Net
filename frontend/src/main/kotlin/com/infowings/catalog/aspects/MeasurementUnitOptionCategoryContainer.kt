package com.infowings.catalog.aspects

import com.infowings.catalog.common.MeasureGroupMap
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.jsStyle

class MeasurementUnitOptionCategoryContainer : RComponent<MeasurementUnitOptionCategoryContainer.Props, MeasurementUnitOptionCategoryContainer.State>() {

    var containerRef: HTMLDivElement? = null

    override fun componentDidMount() {
        setState {
            containerHeight = containerRef?.clientHeight
        }
    }

    override fun RBuilder.render() {
        val optionTopOffset = props.relativeHeight
        val containerHeight = state.containerHeight
        val resultTopOffset = if (containerHeight != null && optionTopOffset != null) optionTopOffset - containerHeight
        else 0
        div(classes = "mu-option-category-container") {
            attrs {
                ref { containerRef = it.unsafeCast<HTMLDivElement?>() }
            }
            attrs.jsStyle.top = "${resultTopOffset}px"
            MeasureGroupMap.values.filter { it.elementGroupMap.containsKey(props.measurementUnit) }
                    .getOrNull(0)?.measureList?.map { measure ->
                div(classes = "mu-option-category-item") {
                    attrs.onClickFunction = {
                        it.preventDefault()
                        it.stopPropagation()
                        props.onUnitClick(measure.name, it)
                    }
                    +measure.name
                }
            }
        }
    }

    interface Props : RProps {
        var measurementUnit: String
        var onUnitClick: (String, Event) -> Unit
        var relativeHeight: Int?
    }

    interface State : RState {
        var containerHeight: Int?
    }
}