@file:JsModule("@blueprintjs/core")

package com.infowings.catalog.wrappers.blueprint

import react.RClass

external val NumericInput: RClass<NumericInputProps>

external interface NumericInputProps : BlueprintComponentProps {

    /**
     * Whether to allow only floating-point number characters in the field, mimicking the native input[type="number"].
     */
    var allowNumericCharactersOnly: Boolean

    /**
     * The position of the buttons with respect to the input field.
     */
    var buttonPosition: Position // only Position.LEFT or Position.RIGHT are valid

    /**
     * Whether the value should be clamped to [min, max] on blur. The value will be clamped to each bound only if the
     * bound is defined. Note that native input[type="number"] controls do NOT clamp on blur.
     */
    var clampValueOnBlur: Boolean

    var disabled: Boolean

    /**
     * Whether the numeric input should take up the full width of its container.
     */
    var fill: Boolean

    var intent: Intent

    /**
     * If set to true, the input will display with larger styling. This is equivalent to setting Classes.LARGE via
     * className on the parent control group and on the child input group.
     */
    var large: Boolean

    var leftIcon: String // IconName

    /**
     * The increment between successive values when shift is held. Pass explicit null value to disable this interaction.
     */
    var majorStepSize: Number?

    var max: Number
    var min: Number

    /**
     * The increment between successive values when alt is held. Pass explicit null value to disable this interaction.
     */
    var minorStepSize: Number?

    var onButtonClick: (valueAsNumber: Number, valueAsString: String) -> Unit

    var onValueChange: (valueAsNumber: Number, valueAsString: String) -> Unit

    var placeholder: String

    var selectAllOnFocus: Boolean

    var selectAllOnIncrement: Boolean

    var stepSize: Number

    var value: String // String | Number
}
