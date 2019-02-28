package com.infowings.catalog.objects.edit.tree.utils

import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.RangeFlagConstants

fun String.isDecimal() = try {
    toDouble()
    true
} catch (e: NumberFormatException) {
    false
}

fun ObjectValueData.validate() {
    when (this) {
        is ObjectValueData.DecimalValue -> validate()
        else -> {
        }
    }
}

fun ObjectValueData.transform(): ObjectValueData = when (this) {
    is ObjectValueData.DecimalValue -> this.transform()
    else -> this
}

fun ObjectValueData.DecimalValue.transform(): ObjectValueData.DecimalValue {
    val leftInfinity = RangeFlagConstants.LEFT_INF.isSet(rangeFlags)
    val rightInfinity = RangeFlagConstants.RIGHT_INF.isSet(rangeFlags)

    if (!leftInfinity && !rightInfinity)
        return this

    return this.copy(valueRepr = if (leftInfinity) "0" else valueRepr, upbRepr = if (rightInfinity) "0" else upbRepr)
}


fun ObjectValueData.DecimalValue.validate() {
    if (RangeFlagConstants.RANGE.isSet(rangeFlags)) {
        if (valueRepr.isEmpty() && upbRepr.isEmpty()) {
            throw InputValidationException("Both ends can't be empty")
        }
        if (valueRepr.isDecimal() && !valueRepr.isDecimal()) {
            throw InputValidationException("This is not a decimal value: $valueRepr")
        }
        if (upbRepr.isDecimal() && !upbRepr.isDecimal()) {
            throw InputValidationException("This is not a decimal value: $upbRepr")
        }

        if (upbRepr.isDecimal() && valueRepr.isDecimal()) {
            val upb = upbRepr.toDouble()
            val lwb = valueRepr.toDouble()
            if (lwb >= upb) {
                throw InputValidationException("Lower bound must be less the upper bound")
            }
        }

    } else {
        if (valueRepr.isEmpty()) {
            throw InputValidationException("Non-range value can't be empty")
        }
        if (!valueRepr.isDecimal()) {
            throw InputValidationException("This is not a decimal value: $valueRepr")
        }
    }

    if (valueRepr.isEmpty() && upbRepr.isEmpty()) {

    }
}

class InputValidationException(message: String) : Exception(message = message);
