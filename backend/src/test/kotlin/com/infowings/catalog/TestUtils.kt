package com.infowings.catalog

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectHistory
import java.util.*
import kotlin.test.assertTrue

fun <T> assertGreater(a: Comparable<T>, b: T) {
    assertTrue(a > b, "$a > $b")
}

fun randomName() = UUID.randomUUID().toString()

fun List<AspectHistory>.forAspect(aspect: AspectData) =
    this.filter { it.fullData.aspectData.name == aspect.name }
