package com.infowings.catalog.aspects

import com.infowings.catalog.common.MeasureGroupMap
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.div
import react.dom.jsStyle

class MeasurementUnitOptionCategoryContainer : RComponent<MeasurementUnitOptionCategoryContainer.Props, RState>() {

    override fun RBuilder.render() {
        val optionTopOffset = props.relativeHeight
        div(classes = "mu-option-category-container") {
            attrs.jsStyle.top = "${optionTopOffset ?: 0}px"
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
}