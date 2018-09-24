package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.ButtonGroup
import com.infowings.catalog.wrappers.blueprint.NumericInput
import react.*
import kotlin.math.pow

class RangedDecimalInput(props: RangedDecimalInput.Props) : RComponent<RangedDecimalInput.Props, RangedDecimalInput.State>(props) {

    override fun State.init(props: Props) {
        lwb = props.lwb
        if (props.lwb < props.upb) {
            upb = props.upb
            prevUpb = ""
        } else {
            upb = null
            prevUpb = Int.MIN_VALUE.toString()
        }
    }


    private fun changeLowerBoundary(number: Number, string: String) {
        val stateUpb = state.upb

        if (stateUpb == null) {
            val old = state.lwb
            setState {
                lwb = string
            }
            if (old != string) props.onUpdate(string, string)
        } else {
            val old = state.lwb
            val newFloat = number.toFloat()
            val oldString = state.lwb

            val newState = if (newFloat < stateUpb.toFloat()) string else oldString

            setState {
                lwb = newState
            }
            if (old != string) props.onUpdate(string, stateUpb)
        }
    }

    private fun changeUpperBoundary(number: Number, string: String) {
        val newFloat = number.toFloat()
        val oldString = state.upb

        val newState = if (state.lwb.toFloat() < newFloat) string else oldString
        val old = state.upb

        setState {
            upb = newState
        }

        if (newState != old) props.onUpdate(state.lwb, string)
    }

    override fun RBuilder.render() {
        val buttonName = "Switch to ${if (state.upb != null) "value" else "range"}"

        val step = 1 * 10.0.pow(- props.lwb.decimalDigits())

        ButtonGroup {
            Button {
                attrs {
                    className = "pt-minimal"
                    disabled = false
                    onClick = {
                        val stateUpb = state.upb
                        if (stateUpb == null) {
                            val newUpb = maxOf(state.lwb.toDouble() + step, state.prevUpb.toDouble()).toString()
                            setState {
                                upb = newUpb
                            }
                            props.onUpdate(state.lwb, newUpb)
                        } else {
                            setState {
                                prevUpb = stateUpb
                                upb = null
                            }
                            props.onUpdate(state.lwb, state.lwb)
                        }
                    }
                }
                +buttonName
            }
        }

        NumericInput {
            val stateUpb = state.upb?.toFloat()

            attrs {
                this.value = state.lwb
                this.majorStepSize = 5.0 * step
                this.minorStepSize = 2.0 * step
                this.stepSize = step
                this.onValueChange = this@RangedDecimalInput::changeLowerBoundary

                max = if (stateUpb != null) stateUpb - step else Double.MAX_VALUE

                this.disabled = disabled
            }
        }

        if (state.upb != null) {
            NumericInput {
                attrs {
                    this.value = state.upb.toString()
                    this.majorStepSize = 5.0 * step
                    this.minorStepSize = 2.0 * step
                    this.stepSize = step
                    this.onValueChange = this@RangedDecimalInput::changeUpperBoundary

                    min = state.lwb.toFloat() - step
                    this.disabled = disabled
                }
            }
        }

    }

    interface Props : RProps {
        var lwb: String
        var upb: String
        var onUpdate: (String, String) -> Unit
        var disabled: Boolean?
    }

    interface State : RState {
        var lwb: String
        var upb: String?
        var prevUpb: String
    }
}

private fun String.decimalDigits(): Int {
    val dotIndex = lastIndexOf('.')
    return if (dotIndex < 0) 0 else length - dotIndex - 1
}

private fun String.trimTailZeros(): String = dropLastWhile { it == '0' }

//fun Float.roundTo(numFractionDigits: Int)
//        = String.format("%.${numFractionDigits}f", toDouble()).toDouble()

fun RBuilder.rangedDecimalInput(handler: RHandler<RangedDecimalInput.Props>) = child(RangedDecimalInput::class, handler)
