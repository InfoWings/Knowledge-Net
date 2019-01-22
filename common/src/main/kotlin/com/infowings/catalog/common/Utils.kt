package com.infowings.catalog.common

fun <U, V> Map<U, V>.getStrict(key: U): V = this[key] ?: throw IllegalStateException("no value for key $key")
