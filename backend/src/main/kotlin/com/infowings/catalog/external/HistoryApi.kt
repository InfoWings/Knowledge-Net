package com.infowings.catalog.external

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectPropertyCardinality
import com.infowings.catalog.storage.ASPECT_CLASS
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/history")
class HistoryApi {

    @GetMapping("aspects")
    fun getAspects(): AspectHistoryList = createTestList()
}

private fun createTestList(): AspectHistoryList {
    val apl = AspectPropertyData("#25:0", "---", "-1", AspectPropertyCardinality.ONE.name)

    val mentalAspect =
        AspectData("#25:47", "Mental", Tonne.name, null, BaseType.Decimal.name, listOf(apl), deleted = true)
    val lengthAspect = AspectData("#1:0", "Length", Metre.name, null, BaseType.Decimal.name, listOf())
    val heightAspect = AspectData("#2:0", "Height", Metre.name, null, BaseType.Decimal.name, listOf())
    val widthAspect = AspectData("#3:0", "Width", Metre.name, null, BaseType.Decimal.name, listOf())
    val volumeAspect = AspectData("4:0", "Volume", CubicCentimetre.name, null, BaseType.Decimal.name, listOf())

    val ap1 = AspectPropertyData("", "", "#25:47", AspectPropertyCardinality.ONE.name)
    val ap2 = AspectPropertyData("", "Height", "#2:0", AspectPropertyCardinality.ONE.name)
    val ap3 = AspectPropertyData("", "Width", "#2:0", AspectPropertyCardinality.ONE.name)
    val ap4 = AspectPropertyData("", "Length", "#1:0", AspectPropertyCardinality.INFINITY.name)
    val ap5 = AspectPropertyData("", "Volume", "#4:0", AspectPropertyCardinality.ZERO.name)
    val ad =
        AspectData("a1", "Tree Measuring", null, null, BaseType.Decimal.name, listOf(ap1))

    val changeList1 = listOf(
        createAspectFieldDelta(AspectField.NAME, null, ad.name),
        createAspectFieldDelta(AspectField.BASE_TYPE, null, ad.baseType),
        createPropertyDelta(null, ap1.toHistoryString())
    )

    val apl2 = ap1.copy(name = "Mental Volume")
    val ad2 = ad.copy(
        properties = ad.properties.subList(1, ad.properties.size).plus(ap2).plus(ap3).plus(apl2),
        version = ad.version + 1
    )
    val changeList2 = listOf(
        createPropertyDelta(null, ap2.toHistoryString()),
        createPropertyDelta(null, ap3.toHistoryString()),
        createPropertyDelta(ap1.toHistoryString(), apl2.toHistoryString())
    )

    val ad3 = ad2.copy(properties = ad2.properties.plus(ap4), version = ad2.version + 1)
    val changeList3 = listOf(
        createPropertyDelta(null, ap4.toHistoryString()),
        createPropertyDelta(null, ap5.toHistoryString())
    )

    val ad4 = ad3.copy(deleted = true, version = ad3.version + 1)
    val changeList4 = mutableListOf(
        createAspectFieldDelta(AspectField.NAME, ad4.name, null),
        createAspectFieldDelta(AspectField.BASE_TYPE, ad4.baseType, null)
    )
    changeList4.addAll(ad4.properties.map { createPropertyDelta(it.toHistoryString(), null) }.toList())

    return listOf(
        createHistElement(
            "Vova",
            EventKind.DELETE,
            changeList4,
            ad4,
            listOf(mentalAspect, heightAspect, widthAspect, volumeAspect, lengthAspect)
        ),
        createHistElement(
            "Sasha",
            EventKind.UPDATE,
            changeList3,
            ad3,
            listOf(mentalAspect, heightAspect, widthAspect, volumeAspect, lengthAspect)
        ),
        createHistElement(
            "Pertya",
            EventKind.UPDATE,
            changeList2,
            ad2,
            listOf(mentalAspect, heightAspect, widthAspect)
        ),
        createHistElement("Vova", EventKind.CREATE, changeList1, ad, listOf(mentalAspect))
    ).toList()
}

private fun createHistElement(
    username: String,
    eventKind: EventKind,
    changes: List<Delta>,
    data: AspectData,
    related: List<AspectData>
) =
    AspectHistory(
        username,
        eventKind,
        ASPECT_CLASS,
        data.name,
        data.deleted,
        System.currentTimeMillis(),
        data.version,
        AspectDataView(data, related),
        changes
    )

private fun AspectPropertyData.toHistoryString(): String {
    //val aspect = aspectService.findById(aspectId)
    val aspectName = "Test"
    return "$name:$cardinality:$aspectName"
}

private fun List<AspectHistory>.toList() =
    AspectHistoryList(this)

private fun createPropertyDelta(before: String?, after: String?) =
    Delta("property", before, after)

private fun createAspectFieldDelta(field: AspectField, before: String?, after: String?) =
    Delta(field.name, before, after)