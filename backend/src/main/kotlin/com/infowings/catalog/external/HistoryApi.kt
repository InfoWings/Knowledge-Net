package com.infowings.catalog.external

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectPropertyCardinality
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.storage.ASPECT_CLASS
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/history")
class HistoryApi(val aspectService: AspectService) {

    @GetMapping("aspects")
    fun getAspects(): AspectHistoryList {
        val ap1 = AspectPropertyData("ap1", "", "-1", AspectPropertyCardinality.ONE.name)
        val ap2 = AspectPropertyData("ap1", "ap2", "-1", AspectPropertyCardinality.ONE.name)
        val ad = AspectData("a1", "n1", Metre.name, null, BaseType.Decimal.name, listOf(ap1, ap2))
        val edited = System.currentTimeMillis()
        val changeList = listOf(
            Delta("name", "n0", ad.name),
            createDeltaFromProperty(0, ap1.copy(name = "oldname").toHistoryString(), ap1.toHistoryString())
        )
        return listOf(
            AspectHistory(
                "Vasya",
                EventKind.UPDATE,
                ASPECT_CLASS,
                ad.name,
                ad.deleted,
                edited,
                ad.version,
                ad,
                changeList
            )
        ).toList()
    }

    private fun AspectPropertyData.toHistoryString(): String {
        //val aspect = aspectService.findById(aspectId)
        val aspectName = "Test"
        return "$name:$cardinality:$aspectId($aspectName)"
    }

    private fun List<AspectHistory>.toList() = AspectHistoryList(this)
}
