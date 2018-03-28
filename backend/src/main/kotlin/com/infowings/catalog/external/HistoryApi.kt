package com.infowings.catalog.external

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.Metre
import com.infowings.catalog.common.history.*
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
        val ad = AspectData("a1", "Aspect Name", Metre.name, null, BaseType.Decimal.name, listOf(ap1, ap2))
        val edited = System.currentTimeMillis()
        val changeList = listOf(
            createAspectFieldDelta(AspectField.NAME, "n0", ad.name),
            createPropertyDelta(
                0,
                ap1.copy(name = "oldname").toHistoryString(),
                ap1.toHistoryString()
            )
        )
        val histElement = AspectHistory(
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
        return listOf(
            histElement,
            histElement.copy(event = EventKind.DELETE),
            histElement.copy(user = "Petya", info = "Aspect2"),
            histElement.copy(user = "Tolya", info = "Aspect1"),
            histElement.copy(version = 3),
            histElement.copy(version = 3, event = EventKind.CREATE)
        ).toList()
    }

    private fun AspectPropertyData.toHistoryString(): String {
        //val aspect = aspectService.findById(aspectId)
        val aspectName = "Test"
        return "$name:$cardinality:$aspectName"
    }

    private fun List<AspectHistory>.toList() =
        AspectHistoryList(this)
}

private fun createPropertyDelta(propertyNumber: Int, before: String?, after: String?) =
    Delta(AspectPropertyHistory(propertyNumber), before, after)

private fun createAspectFieldDelta(field: AspectField, before: String?, after: String?) =
    Delta(AspectFieldWrapper(field), before, after)