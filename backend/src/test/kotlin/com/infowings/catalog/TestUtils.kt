package com.infowings.catalog

import java.util.*
import kotlin.test.assertTrue

fun <T> assertGreater(a: Comparable<T>, b: T) {
    assertTrue(a > b, "$a > $b")
}

fun randomName() = UUID.randomUUID().toString()