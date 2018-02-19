package com.infowings.catalog.external

import com.infowings.catalog.data.Measure
import com.infowings.catalog.data.MeasureGroup
import com.infowings.catalog.data.MeasureGroupMap
import com.infowings.common.MeasureData
import com.infowings.common.MeasureGroupData
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/measure")
class MeasureApi {

    @GetMapping("all")
    fun getAllMeasures(): List<MeasureGroupData> = MeasureGroupMap.values.map { it.toDto() }
}

private fun MeasureGroup<*>.toDto() = MeasureGroupData(name, measureList.map { it.toDto() })
private fun Measure<*>.toDto() = MeasureData(name, symbol, baseType.name)