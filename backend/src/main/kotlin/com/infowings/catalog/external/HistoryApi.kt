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

        val apl = AspectPropertyData("ap-1", "", "null", AspectPropertyCardinality.ONE.name)
        val related = AspectData("-1", "As2", Tonne.name, null, BaseType.Decimal.name, listOf(apl), deleted = true)

        val ap1 = AspectPropertyData("ap1", "", "-1", AspectPropertyCardinality.ONE.name)
        val ap2 = AspectPropertyData("ap2", "Property1", "-1", AspectPropertyCardinality.ONE.name)
        val ap3 = AspectPropertyData("ap3", "Property2", "-1", AspectPropertyCardinality.ONE.name)
        val ap4 = AspectPropertyData("ap4", "", "-1", AspectPropertyCardinality.INFINITY.name)
        val ap5 = AspectPropertyData("ap5", "Property5", "-1", AspectPropertyCardinality.ZERO.name)
        val ad =
            AspectData("a1", "Aspect Name", Metre.name, null, BaseType.Decimal.name, listOf(ap1, ap2, ap3, ap4, ap5))
        val edited = System.currentTimeMillis()
        val changeList = listOf(
            createAspectFieldDelta(AspectField.NAME, "n0", ad.name),
            createAspectFieldDelta(AspectField.BASE_TYPE, null, ad.baseType),
            createPropertyDelta(
                0,
                ap1.copy(name = "oldname").toHistoryString(),
                ap1.toHistoryString()
            ),
            createPropertyDelta(
                1,
                ap1.copy(name = "prop").toHistoryString(),
                null
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
            AspectDataView(ad, listOf(related)),
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
    Delta("property[$propertyNumber]", before, after)

private fun createAspectFieldDelta(field: AspectField, before: String?, after: String?) =
    Delta(field.name, before, after)