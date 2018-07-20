package com.infowings.catalog.utils

import java.util.*

fun <T> Optional<T>.toNullable(): T? = this.orElse(null)
