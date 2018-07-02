package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectDoesNotExist
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.HistoryFact

class AspectDeltaConstructor(val aspectService: AspectService) {

    fun createDiff(before: AspectData, after: AspectData, mainFact: HistoryFact): AspectHistory {

        var diffs = mutableListOf<FieldDelta>()

        diffs.addAll(fieldsDiff(mainFact, before, after))

        if (before.subject != after.subject) {
            diffs.add(
                createAspectFieldDelta(
                    mainFact.event.type,
                    AspectField.SUBJECT,
                    before.subject?.name,
                    after.subject?.name
                )
            )
        }

        if (before.refBookName != after.refBookName) {
            diffs.add(
                createAspectFieldDelta(
                    mainFact.event.type,
                    AspectField.REFERENCE_BOOK,
                    before.refBookName,
                    after.refBookName
                )
            )
        }

        diffs.addAll(propertiesDiff(before, after))

        diffs = diffs.filter { it.after != it.before }.toMutableList()

        return createHistoryElement(mainFact.event, diffs, after, after.properties.map { getAspect(it.aspectId) })
    }

    private fun fieldsDiff(mainFact: HistoryFact, before: AspectData, after: AspectData): List<FieldDelta> =
        mainFact.payload.data.mapNotNull { (fieldName, _) ->

            when (AspectField.valueOf(fieldName)) {
                AspectField.MEASURE -> createAspectFieldDelta(
                    mainFact.event.type,
                    AspectField.MEASURE.view,
                    before.measure,
                    after.measure
                )
                AspectField.BASE_TYPE -> createAspectFieldDelta(
                    mainFact.event.type,
                    AspectField.BASE_TYPE.view,
                    before.baseType,
                    after.baseType
                )
                AspectField.NAME -> createAspectFieldDelta(
                    mainFact.event.type,
                    AspectField.NAME.view,
                    before.name,
                    after.name
                )
                AspectField.DESCRIPTION -> createAspectFieldDelta(
                    mainFact.event.type,
                    AspectField.DESCRIPTION.view,
                    before.description,
                    after.description
                )
            }
        }

    private fun propertiesDiff(before: AspectData, after: AspectData): List<FieldDelta> {

        val diffs = mutableListOf<FieldDelta>()

        val beforePropertyIdSet = before.properties.map { it.id }.toSet()
        val afterPropertyIdSet = after.properties.map { it.id }.toSet()

        /* TODO:
         * по идее сервис типа AspectDeltaConstructor должен выдавать структуру отличий,
         * а метод propertyDiff выдает вариант внешнего представления отличий.
         * В частности, если в при нынешнем дизайне захочется нописать внешний скрипт, который
         * бы запрашивал историю аспектов через rest api и выдал отчет о добавленных пропертях,
         * придется парсить поле after, делалая предположения о его структуре
         */

        diffs.addAll(beforePropertyIdSet.intersect(afterPropertyIdSet).flatMap { id ->
            propertyDiff(
                id,
                EventType.UPDATE,
                before,
                after
            )
        })
        diffs.addAll(beforePropertyIdSet.subtract(afterPropertyIdSet).flatMap { id ->
            propertyDiff(
                id,
                EventType.DELETE,
                before,
                after
            )
        })
        diffs.addAll(afterPropertyIdSet.subtract(beforePropertyIdSet).flatMap { id ->
            propertyDiff(
                id,
                EventType.CREATE,
                before,
                after
            )
        })

        return diffs
    }

    private fun propertyDiff(
        id: String,
        eventType: EventType,
        before: AspectData,
        after: AspectData
    ): List<FieldDelta> {
        val propertyDiff = createAspectFieldDelta(eventType, after[id].deltaName, before[id]?.view, after[id]?.view)

        if (before[id]?.description != after[id]?.description) {

            val descriptionDiff =
                createAspectFieldDelta(
                    eventType,
                    after[id].deltaName + "(Description)",
                    before[id]?.description,
                    after[id]?.description
                )

            return listOf(propertyDiff, descriptionDiff)
        }
        return listOf(propertyDiff)
    }

    private fun createHistoryElement(
        event: HistoryEventData,
        changes: List<FieldDelta>,
        data: AspectData,
        related: List<AspectData>
    ) = AspectHistory(
        event,
        data.name,
        data.deleted,
        AspectDataView(data, related),
        changes
    )

    private val AspectPropertyData?.deltaName
        get() = "${AspectField.PROPERTY} ${this?.name ?: " "}"

    private val AspectPropertyData.view: String
        get() {
            val cardinalityLabel = PropertyCardinality.valueOf(cardinality).label
            return "$name ${getAspect(aspectId).name} : [$cardinalityLabel]"
        }

    private fun getAspect(aspectId: String): AspectData = try {
        aspectService.findById(aspectId)
    } catch (e: AspectDoesNotExist) {
        AspectData(id = aspectId, name = "'Aspect removed'")
    }
}