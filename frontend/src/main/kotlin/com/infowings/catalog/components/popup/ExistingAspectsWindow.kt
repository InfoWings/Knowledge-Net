package com.infowings.catalog.components.popup

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
        fun elemByAspectName(v: String): ReactElement {
            return buildElement {
                label {
                    +v
                }
            } ?: "???".asReactElement()
        }

        fun elemByAspectDesc(v: String, description: String?): ReactElement {
            return buildElement {
                div {
                    descriptionComponent(
                        className = "aspect-tree-view--description-icon",
                        description = description
                    )

                    span {
                        +" "
                    }

                    label {
                        +v
                    }
                }
            } ?: "???".asReactElement()
        }


        fun elemByRefBookValue(aspectName: String, value: String): ReactElement {
            return buildElement {
                label {
                    +"$aspectName:[$value]"
                }
            } ?: "???".asReactElement()
        }

        fun elemByRefBookDesc(aspectName: String, value: String, description: String?): ReactElement {
            return buildElement {
                div {
                    descriptionComponent(
                        className = "aspect-tree-view--description-icon",
                        description = description
                    )

                    span {
                        +" "
                    }

                    label {
                        +"$aspectName:[$value]"
                    }
                }
            } ?: "???".asReactElement()

        }

        fun elemByProperty(aspectName: String, propName: String, subAspectName: String): ReactElement {
            return buildElement {
                label {
                    +"$aspectName.$propName-$subAspectName"
                }
            } ?: "???".asReactElement()
        }

        fun aspectOption(name: String) = jsObject<AspectOption> {
            this.name = name
            this.aspectName = elemByAspectName(name)
        }

        fun aspectDescOption(hint: AspectHint) = jsObject<AspectOption> {
            this.name = hint.name
            this.aspectName = elemByAspectDesc(hint.name, hint.description)
        }

        fun propertyOption(hint: AspectHint) = jsObject<AspectOption> {
            this.name = hint.name
            this.aspectName = elemByProperty(hint.name, hint.propertyName ?: "?", hint.subAspectName ?: "?")
        }

        fun refBookValueOption(hint: AspectHint) = jsObject<AspectOption> {
            this.name = hint.name
            this.aspectName = elemByRefBookValue(hint.name, hint.refBookItem ?: "?")
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
                    value = props.message
                    labelKey = "aspectName"
                    valueKey = ""
                    clearable = false
                    resetValue = null
                    disabled = false// props.disabled
                    options = (props.hints.byAspectName.map { aspectOption(it.name) } +
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
        var message: String
    }
}

fun RBuilder.existingAspectWindow(block: RHandler<ExistingAspectsWindow.Props>) =
    child(ExistingAspectsWindow::class, block)
