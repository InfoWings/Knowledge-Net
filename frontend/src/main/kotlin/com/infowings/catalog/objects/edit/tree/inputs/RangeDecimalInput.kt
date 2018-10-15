package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.ButtonGroup
import com.infowings.catalog.wrappers.blueprint.NumericInput
import react.*
import kotlin.math.pow

class RangedDecimalInput(props: RangedDecimalInput.Props) : RComponent<RangedDecimalInput.Props, RangedDecimalInput.State>(props) {
    override fun State.init(props: Props) {
        lwb = props.lwb
        if (props.lwb != props.upb) {
            upb = props.upb
            prevUpb = ""
        } else {
            upb = null
            prevUpb = Int.MIN_VALUE.toString()
        }
    }


    private fun normalized(s: String): String {
        val nDots = s.filter { it == '.' }.sumBy { 1 }

        if (s.isEmpty() || nDots > 1) throw NumberFormatException()

        return if (s[s.length - 1] == '.') s.substring(0, s.length - 1) else s
    }

    private fun changeLowerBoundary(number: Number, string: String) {
        val normString = try {
            normalized(string)
        } catch (e: NumberFormatException) {
            return
        }
        if (normString == state.lwb) return
        val oldString = state.upb

        if (normString.toDouble() < state.upb?.toDouble() ?: Double.MAX_VALUE) {
            setState {
                lwb = normString
            }
            val upb = state.upb
            if (oldString != string) props.onUpdate(string, upb ?: string)
        } else {
            props.onUpdate(state.lwb, state.upb ?: state.lwb)
        }
    }

    private fun changeUpperBoundary(number: Number, string: String) {
        val normString = try {
            normalized(string)
        } catch (e: NumberFormatException) {
            return
        }
        if (normString == state.upb) return

        val oldString = state.upb

        if (normString.toDouble() > state.lwb.toDouble()) {
            setState {
                upb = normString
            }
            if (normString != oldString) props.onUpdate(state.lwb, normString)
        } else {
            props.onUpdate(state.lwb, state.upb ?: state.lwb)
        }
    }

    private fun step(): Double {
        val stepLwb = 1 * 10.0.pow(-state.lwb.decimalDigits())
        val stepUpb = 1 * 10.0.pow(-(state.upb?.decimalDigits() ?: 0))

        return maxOf(minOf(stepLwb, stepUpb), 0.0001)
    }

    override fun RBuilder.render() {
        val buttonName = "Switch to ${if (state.upb != null) "value" else "range"}"

        val step = step()
        val toDisable = props.disabled

        ButtonGroup {
            Button {
                attrs {
                    className = "pt-minimal"
                    disabled = toDisable ?: false
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

        val newState = state

        NumericInput {
            attrs {
                this.value = newState.lwb
                this.majorStepSize = 5.0 * step
                this.minorStepSize = 2.0 * step
                this.stepSize = step
                this.onValueChange = this@RangedDecimalInput::changeLowerBoundary

                this.disabled = toDisable ?: false
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
                    this.disabled = toDisable ?: false
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

fun RBuilder.rangedDecimalInput(handler: RHandler<RangedDecimalInput.Props>) = child(RangedDecimalInput::class, handler)
