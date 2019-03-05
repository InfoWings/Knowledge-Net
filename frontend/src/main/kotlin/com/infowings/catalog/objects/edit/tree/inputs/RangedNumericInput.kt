package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.ButtonGroup
import com.infowings.catalog.wrappers.blueprint.NumericInput
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

    @Suppress("UNUSED_PARAMETER")
    private fun changeLowerBoundary(v: Number, v2: String) {
        val newLwb = v.toInt()
        val oldLwb = state.lwb

        if (newLwb == oldLwb) return

        if (newLwb < state.upb ?: Int.MAX_VALUE) {
            val upb = state.upb
            props.onUpdate(newLwb, upb ?: newLwb)
        } else {
            val upb = state.upb
            props.onUpdate(state.lwb, upb ?: state.lwb)

        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun changeUpperBoundary(v: Number, v2: String) {
        val newUpb = v.toInt()
        val oldUpb = state.upb

        if (newUpb == oldUpb) return

        if (newUpb > state.lwb) {
            setState {
                upb = newUpb
            }
            props.onUpdate(state.lwb, newUpb)
        } else {
            props.onUpdate(state.lwb, state.upb ?: state.lwb)
        }
    }

    override fun RBuilder.render() {
        val buttonName = "Switch to ${if (state.upb != null) "value" else "range"}"

        ButtonGroup {
            Button {
                attrs {
                    className = "bp3-minimal"
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