package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.ButtonGroup
import com.infowings.catalog.wrappers.blueprint.NumericInput
import react.*

class RangedDecimalInput(props: RangedDecimalInput.Props) : RComponent<RangedDecimalInput.Props, RangedDecimalInput.State>(props) {
    override fun State.init(props: Props) {
        lwb = props.lwb
        upb = props.upb
        isRange = props.isRange
    }

    private fun changeLowerBoundary(number: Number, string: String) {
        setState {
            lwb = string
        }
        println("changed l: $string - ${props.upb}, ${props.isRange}")
        props.onUpdate(string, props.upb, props.isRange)
    }

    private fun changeUpperBoundary(number: Number, string: String) {
        setState {
            upb = string
        }
        println("changed r: ${props.lwb} - $string, ${props.isRange}")
        props.onUpdate(props.lwb, string, props.isRange)
    }

    override fun RBuilder.render() {
        println("range render: ${props.lwb} - ${props.upb}, ${props.isRange}")
        val buttonName = "Switch to ${if (state.isRange) "value" else "range"}"

        val step = 1

        ButtonGroup {
            Button {
                attrs {
                    className = "pt-minimal"
                    this.disabled = props.disabled ?: false
                    onClick = {
                        if (!state.isRange) {
                            setState {
                                upb = state.lwb
                                isRange = true
                            }
                            props.onUpdate(props.lwb, props.upb, true)
                        } else {
                            setState {
                                isRange = false
                            }
                            props.onUpdate(props.lwb, props.upb, false)
                        }
                    }
                }
                +buttonName
            }

        }

        NumericInput {
            attrs {
                this.value = props.lwb
                this.majorStepSize = 5.0 * step
                this.minorStepSize = 2.0 * step
                this.stepSize = step
                this.onValueChange = this@RangedDecimalInput::changeLowerBoundary
                this.disabled = props.disabled ?: false
                placeholder = if (state.isRange) "-Infinity" else ""
            }
        }

        if (state.isRange) {
            NumericInput {
                attrs {
                    this.value = props.upb
                    this.majorStepSize = 5.0 * step
                    this.minorStepSize = 2.0 * step
                    this.stepSize = step
                    this.onValueChange = this@RangedDecimalInput::changeUpperBoundary
                    this.disabled = props.disabled ?: false
                    placeholder = "Infinity"
                }
            }
        }
    }

    interface Props : RProps {
        var lwb: String
        var upb: String
        var isRange: Boolean
        var onUpdate: (String, String, Boolean) -> Unit
        var disabled: Boolean?
    }

    interface State : RState {
        var lwb: String
        var upb: String?
        var isRange: Boolean
    }
}


fun RBuilder.rangedDecimalInput(handler: RHandler<RangedDecimalInput.Props>) = child(RangedDecimalInput::class, handler)
