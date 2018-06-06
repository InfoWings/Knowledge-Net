package com.infowings.catalog

import kotlin.test.assertTrue


fun <T> assertGreater(a: Comparable<T>, b: T) {
    assertTrue(a > b, "$a > $b")
}
