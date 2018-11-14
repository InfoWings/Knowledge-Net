package com.infowings.catalog.components.popup

import com.infowings.catalog.aspects.*
import com.infowings.catalog.common.AspectHint
import com.infowings.catalog.common.AspectsHints
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.wrappers.react.asReactElement
import com.infowings.catalog.wrappers.react.label
import com.infowings.catalog.wrappers.select.SelectOption
import com.infowings.catalog.wrappers.select.commonSelect
import kotlinext.js.jsObject
import react.*
import react.dom.div
import react.dom.span


interface AspectOption : SelectOption {
    var name: String
    var aspectName: ReactElement
}

class ExistingAspectsWindow : RComponent<ExistingAspectsWindow.Props, RState>() {

    override fun RBuilder.render() {
        fun aspectOption(name: String) = jsObject<AspectOption> {
            this.name = name
            this.aspectName = elemByAspectName(name)
        }

        fun aspectDescOption(hint: AspectHint) = jsObject<AspectOption> {
            this.name = hint.name
            this.aspectName = elemByAspectDesc(hint.subjectName ?: "???", hint.description)
        }

        fun propertyOption(hint: AspectHint) = jsObject<AspectOption> {
            this.name = hint.name
            this.aspectName = elemByProperty(hint.name, hint.propertyName ?: "?", hint.subAspectName ?: "?")
        }

        fun refBookValueOption(hint: AspectHint) = jsObject<AspectOption> {
            this.name = hint.name
            this.aspectName = elemByRefBookValue(hint.name ?: "???", hint.refBookItem ?: "?")
        }

        fun refBookDescOption(hint: AspectHint) = jsObject<AspectOption> {
            this.name = hint.name
            this.aspectName = elemByRefBookDesc(hint.name, hint.refBookItem ?: "?", hint.refBookItemDesc)
        }

        if (props.hints.byAspectName.size + props.hints.byProperty.size + props.hints.byRefBookDesc.size +
            props.hints.byRefBookValue.size + props.hints.byAspectDesc.size > 0
        ) {

            commonSelect<AspectOption> {
                attrs {
                    openOnFocus = true
                    placeholder = "${props.hints.byAspectName.size + props.hints.byProperty.size + props.hints.byRefBookDesc.size +
                            props.hints.byRefBookValue.size + props.hints.byAspectDesc.size} similar"
                    className = "aspect-table-select similar-aspects"
                    value = ""
                    labelKey = "aspectName"
                    valueKey = ""
                    clearable = false
                    resetValue = null
                    disabled = false// props.disabled
                    options = (props.hints.byAspectName.map { aspectOption(it.subjectName ?: "???") } +
                            props.hints.byProperty.map { propertyOption(it) } +
                            props.hints.byRefBookValue.map { refBookValueOption(it) } +
                            props.hints.byAspectDesc.map { aspectDescOption(it) } +
                            props.hints.byRefBookDesc.map { refBookDescOption(it) }
                            ).toTypedArray()
                }
            }
        }
    }

    interface Props : RProps {
        var hints: AspectsHints
    }
}

fun RBuilder.existingAspectWindow(block: RHandler<ExistingAspectsWindow.Props>) =
    child(ExistingAspectsWindow::class, block)
