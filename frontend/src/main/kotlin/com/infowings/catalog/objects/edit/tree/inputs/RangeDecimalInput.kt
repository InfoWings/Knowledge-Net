package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.common.RangeFlagConstants
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.ButtonGroup
import com.infowings.catalog.wrappers.blueprint.NumericInput
import react.*
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt

class RangedDecimalInput(props: RangedDecimalInput.Props) : RComponent<RangedDecimalInput.Props, RangedDecimalInput.State>(props) {
    override fun State.init(props: Props) {
        lwb = props.lwb
        upb = props.upb
        leftInfinity = props.leftInfinity
        rightInfinity = props.rightInfinity
        prevUpb = props.upb
    }

    private fun changeLowerBoundary(number: Number, string: String) {
        val upbPrev = props.upb
        val lwbPrev = props.lwb
        if (string.trim().isEmpty()) {
            // switch to infinity mode

            if (props.rightInfinity) {
                setState {
                    this.lwb = lwbPrev
                }

                return
            }

            val prevInfinity = props.leftInfinity
            setState {
                leftInfinity = true
                this.lwb = ""
            }
            if (!prevInfinity) {
                props.onUpdate(string, if (props.upb == null) props.lwb else props.upb ?: "???", calcRangeFlags(true, props.rightInfinity))
            }
        } else {
            if (props.leftInfinity) {
                // we are in infinity mode but try to switch to numeric mode
                if (upbPrev == null) {
                    setState {
                        leftInfinity = false
                        lwb = "0"
                    }
                    props.onUpdate("0", props.upb, 0)
                } else {
                    if (string.toDouble() < upbPrev.toDouble()) {
                        setState {
                            leftInfinity = false
                            lwb = string
                        }
                        props.onUpdate(string, props.upb, 0)
                    } else {
                        val newLwb = upbPrev.smallDelta(up = false)
                        setState {
                            leftInfinity = false
                            lwb = newLwb
                        }
                        props.onUpdate(newLwb, props.upb, 0)
                    }

                }
            } else {
                val oldString = props.lwb

                if (props.rightInfinity || upbPrev == null || string.toDouble() < upbPrev.toDouble()) {
                    setState {
                        lwb = string
                    }
                    if (string != oldString) props.onUpdate(string, upbPrev ?: string, calcRangeFlags())
                } else {
                    setState {
                        lwb = lwbPrev
                    }
                }
            }
        }
    }

    private fun changeUpperBoundary(number: Number, string: String) {
        val upbPrev = props.upb
        val lwbPrev = props.lwb
        if (string.trim().isEmpty()) {
            // switch to infinity mode
            if (props.leftInfinity) {
                setState {
                    this.upb = upbPrev
                }
                return
            }

            val prevInfinity = props.rightInfinity
            setState {
                rightInfinity = true
                upb = ""
            }
            if (!prevInfinity) {
                props.onUpdate(props.lwb, if (props.upb == null) props.lwb else string, calcRangeFlags(props.leftInfinity, true))
            }
        } else {
            if (props.rightInfinity) {
                // we are in infinity mode but try to switch to numeric mode
                if (string.toDouble() < props.lwb.toDouble()) {
                    val newUpb = props.lwb.smallDelta(up = true)
                    setState {
                        rightInfinity = false
                        upb = newUpb
                    }
                    props.onUpdate(props.lwb, newUpb, 0)
                } else {
                    setState {
                        rightInfinity = false
                        upb = string
                    }
                    props.onUpdate(props.lwb, string, 0)
                }
            } else {
                val oldString = props.upb

                if (props.leftInfinity || string.contains('e') || string.toDouble() > props.lwb.toDouble()) {
                    setState {
                        upb = string
                    }
                    if (string != oldString) props.onUpdate(props.lwb, string, calcRangeFlags())
                } else {
                    setState {
                        lwb = lwbPrev
                    }
                }
            }
        }
    }

    fun calcRangeFlags(): Int {
        var result = 0
        if (state.leftInfinity) result += RangeFlagConstants.LEFT_INF.bitmask
        if (state.rightInfinity) result += RangeFlagConstants.RIGHT_INF.bitmask

        return result
    }

    fun calcRangeFlags(left: Boolean, right: Boolean): Int {
        var result = 0
        if (left) result += RangeFlagConstants.LEFT_INF.bitmask
        if (right) result += RangeFlagConstants.RIGHT_INF.bitmask

        return result
    }

    private fun step(): Double {
        val stepLwb = 1 * 10.0.pow(-state.lwb.decimalDigits())
        val stepUpb = 1 * 10.0.pow(-(state.upb?.decimalDigits() ?: 0))

        return maxOf(minOf(stepLwb, stepUpb), 0.0001)
    }

    private fun hasInfinity() = props.leftInfinity || props.rightInfinity

    private fun isRangeMode() = props.upb != props.lwb || hasInfinity()

    override fun RBuilder.render() {
        val buttonName = "Switch to ${if (isRangeMode()) "value" else "range"}"

        val step = 0.1
        val toDisable = props.disabled

        ButtonGroup {
            Button {
                attrs {
                    className = "pt-minimal"
                    disabled = toDisable ?: false
                    onClick = {
                        val stateUpb = props.upb
                        if (stateUpb == props.lwb) {
                            val minUpb = props.lwb.smallDelta(up = true)
                            val newUpb = when {
                                state.prevUpb.trim().isEmpty() -> minUpb
                                state.prevUpb.toDouble() > minUpb.toDouble() -> state.prevUpb
                                else -> minUpb
                            }
                            setState {
                                upb = newUpb
                            }
                            props.onUpdate(props.lwb, newUpb, 0)
                        } else {
                            when {
                                props.leftInfinity -> {
                                    setState {
                                        prevUpb = "0"
                                        upb = "0"
                                        lwb = "0"
                                        leftInfinity = false
                                        rightInfinity = false
                                    }
                                    props.onUpdate("0", "0", 0)
                                }
                                props.rightInfinity -> {
                                    setState {
                                        prevUpb = props.upb
                                        upb = lwb
                                        leftInfinity = false
                                        rightInfinity = false
                                    }
                                    props.onUpdate(props.lwb, props.lwb, 0)
                                }
                                else -> {
                                    setState {
                                        prevUpb = props.upb
                                        upb = lwb
                                    }
                                    props.onUpdate(props.lwb, props.lwb, 0)
                                }
                            }
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
                this.disabled = toDisable ?: false
            }
        }

        if (isRangeMode()) {
             NumericInput {
                 attrs {
                     this.value = if (props.rightInfinity) "" else props.upb
                     this.majorStepSize = 5.0 * step
                     this.minorStepSize = 2.0 * step
                     this.stepSize = step
                     this.onValueChange =  this@RangedDecimalInput::changeUpperBoundary
                     this.disabled = toDisable ?: false
                 }
             }
        }
    }

    interface Props : RProps {
        var lwb: String
        var upb: String
        var leftInfinity: Boolean
        var rightInfinity: Boolean
        var onUpdate: (String, String, Int) -> Unit
        var disabled: Boolean?
    }

    interface State : RState {
        var lwb: String
        var upb: String?
        var leftInfinity: Boolean
        var rightInfinity: Boolean
        var prevUpb: String
    }
}

private fun isNear(dbl: Double, int: Int): Boolean = (dbl - int).absoluteValue < 1.0e-4

private fun Double.isNearToInt(): Boolean = isNear(this, this.roundToInt())

private fun String.smallDelta(up: Boolean): String {
    val coeff = if (up) 1 else -1
    val prec = decimalDigits()

    if (prec == 0) {
        return (toDouble().roundToInt() + 1*coeff).toString()
    } else {
        val v = ((toDouble() * (10.0.pow(prec))).roundToInt() + 1*coeff).toString()
        return v.take(v.length - prec) + "." + v.drop(v.length - prec)
    }
}

private fun String.decimalDigits(): Int {
    if (this.trim().isEmpty()) return 0

    val dbl = this.toDouble()
    if (dbl.isNearToInt()) {
        return 0
    }

    val nDigits = (1..6).indexOfFirst { degree: Int ->
        (dbl * 10.0.pow(degree)).isNearToInt()
    }
    return if (nDigits < 0) 7 else nDigits + 1
}

fun RBuilder.rangedDecimalInput(handler: RHandler<RangedDecimalInput.Props>) = child(RangedDecimalInput::class, handler)
