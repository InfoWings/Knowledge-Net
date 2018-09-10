package com.infowings.catalog

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectHistory
import java.util.*

fun randomName(base: String = "") = "$base${UUID.randomUUID()}"

fun List<AspectHistory>.forAspect(aspect: AspectData) =
    this.filter { it.fullData.aspectData.name == aspect.name }
