package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.OpenDomain
import com.infowings.catalog.data.history.HistoryFact
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.MutableSnapshot
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.ASPECT_PROPERTY_CLASS
import com.infowings.catalog.storage.SUBJECT_CLASS
import java.util.concurrent.CopyOnWriteArrayList

private val logger = loggerFor<AspectHistoryProvider>()

class AspectHistoryProvider(
    private val historyService: HistoryService,
    private val aspectDeltaConstructor: AspectDeltaConstructor,
    private val aspectConstructor: AspectConstructor
) {

    fun getAllHistory(): List<AspectHistory> {
        val bothFacts = historyService.allTimeline(listOf(ASPECT_CLASS, ASPECT_PROPERTY_CLASS))

        val factsByClass = bothFacts.groupBy { it.event.entityClass }
        val aspectFacts = factsByClass[ASPECT_CLASS] ?: emptyList()
        val aspectFactsByEntity = aspectFacts.groupBy { it.event.entityId }
        val propertyFacts = factsByClass[ASPECT_PROPERTY_CLASS] ?: emptyList()
        val propertyFactsBySession = propertyFacts.groupBy { it.event.sessionId }

        val propertySnapshots = propertyFacts.map { it.event.entityId to MutableSnapshot() }.toMap()

        val events = logTime(logger, "processing aspect event groups") {
            aspectFactsByEntity.values.flatMap {  aspectFacts ->

                val snapshot = MutableSnapshot()

                var aspectDataAccumulator = AspectData(name = "")

                val versionList2 = listOf(AspectData(id = null, name = "")) + aspectFacts.map { aspectFact ->
                    if (!aspectFact.event.type.isDelete()) {
                        snapshot.apply(aspectFact.payload)
                        val propertyFacts = propertyFactsBySession[aspectFact.event.sessionId]
                        propertyFacts?.forEach { propertyFact ->
                            propertySnapshots[propertyFact.event.entityId]?.apply(propertyFact.payload)
                        }

                    }

                    val baseType = snapshot.data[AspectField.BASE_TYPE.name]

                    val properties = (snapshot.links[AspectField.PROPERTY]?.toSet() ?: emptySet()).map {
                        val propId = it.toString()
                        val propSnapshot = propertySnapshots[propId] ?: MutableSnapshot()
                        AspectPropertyData(id = propId,
                            name = propSnapshot.data[AspectPropertyField.NAME.name] ?: "",
                            aspectId = propSnapshot.data[AspectPropertyField.ASPECT.name] ?: "",
                            cardinality = propSnapshot.data[AspectPropertyField.CARDINALITY.name] ?: "",
                            description = propSnapshot.data[AspectPropertyField.DESCRIPTION.name],
                            version = 1)
                    }

                    AspectData(id = null,
                        name = snapshot.data.getValue(AspectField.NAME.name),
                        description = snapshot.data[AspectField.DESCRIPTION.name],
                        baseType = snapshot.data[AspectField.BASE_TYPE.name],
                        domain = baseType?.let { OpenDomain(BaseType.restoreBaseType(it)).toString() },
                        measure = snapshot.data[AspectField.BASE_TYPE.name],
                        properties = properties
                    )
                }

                val versionList: List<AspectData> =
                    listOf(aspectDataAccumulator).plus(aspectFacts.map { fact ->

                        val relatedFacts = propertyFactsBySession[fact.event.sessionId] ?: emptyList()

                        aspectDataAccumulator = aspectConstructor.toNextVersion(aspectDataAccumulator, fact, relatedFacts)

                        return@map aspectDataAccumulator
                    })


                logger.info("versions cmp:")
                versionList.zip(versionList2).forEach {
                    logger.info("1: " + it.first)
                    logger.info("2: " + it.second)
                    logger.info("1 == 2: {${it.first == it.second}}")
                }


                return@flatMap logTime(logger, "aspect diffs creation for aspect ${aspectFacts.firstOrNull()?.event?.entityId}") {
                    versionList.zipWithNext().zip(aspectFacts)
                        .map { aspectDeltaConstructor.createDiff(it.first.first, it.first.second, it.second) }
                }
            }
        }

        return events.sortedByDescending { it.event.timestamp }
    }
}