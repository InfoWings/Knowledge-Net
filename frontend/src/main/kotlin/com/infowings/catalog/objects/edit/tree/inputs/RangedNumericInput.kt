package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.wrappers.blueprint.*
import react.*

class RangedNumericInput(props: RangedNumericInput.Props) : RComponent<RangedNumericInput.Props, RangedNumericInput.State>(props) {

    override fun State.init(props: Props) {
        lwb = props.lwb
        if (props.lwb < props.upb) {
            upb = props.upb
            prevUpb = 0
        } else {
            upb = null
            prevUpb = Int.MIN_VALUE
        }
    }

    private fun changeLowerBoundary(v: Number, v2: String) {
        val newLwb = v.toInt()
        val oldLwb = state.lwb
        setState {
            lwb = newLwb
        }
        val upb = state.upb
        if (newLwb != oldLwb) props.onUpdate(newLwb, upb ?: newLwb)
    }

    private fun changeUpperBoundary(v: Number, v2: String) {
        val newUpb = v.toInt()
        val oldUpb = state.upb
        setState {
            upb = newUpb
        }
        if (newUpb != oldUpb) props.onUpdate(state.lwb, newUpb)
    }

    override fun RBuilder.render() {
        val buttonName = "Switch to ${if (state.upb != null) "value" else "range"}"

        ButtonGroup {
            Button {
                attrs {
                    className = "pt-minimal"
                    disabled = false
                    onClick = {
                        val stateUpb = state.upb
                        if (stateUpb == null) {
                            val newUpb = maxOf(state.lwb + 1, state.prevUpb)
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
            val stateUpb = state.upb
            attrs {
                this.value = state.lwb.toString()
                this.onValueChange = this@RangedNumericInput::changeLowerBoundary
                max = if (stateUpb != null) stateUpb - 1 else Int.MAX_VALUE
                majorStepSize = 1
                minorStepSize = 1
                stepSize = 1
                this.disabled = disabled
            }
        }

        if (state.upb != null) {
            NumericInput {
                attrs {
                    this.value = state.upb.toString()
                    this.onValueChange = this@RangedNumericInput::changeUpperBoundary
                    min = state.lwb + 1
                    majorStepSize = 1
                    minorStepSize = 1
                    stepSize = 1
                    this.disabled = disabled
                }
            }
        }

    }

    interface Props : RProps {
        var lwb: Int
        var upb: Int
        var onUpdate: (Int, Int) -> Unit
        var disabled: Boolean?
    }

    interface State : RState {
        var lwb: Int
        var upb: Int?
        var prevUpb: Int
    }
}

fun RBuilder.rangedNumericInput(handler: RHandler<RangedNumericInput.Props>) = child(RangedNumericInput::class, handler)