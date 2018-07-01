package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.OpenDomain
import com.infowings.catalog.data.history.HistoryFact
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.MutableSnapshot
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.reference.book.ReferenceBookDao
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.ASPECT_PROPERTY_CLASS
import com.infowings.catalog.storage.SUBJECT_CLASS
import com.infowings.catalog.storage.id
import java.util.concurrent.CopyOnWriteArrayList

private val logger = loggerFor<AspectHistoryProvider>()

class AspectHistoryProvider(
    private val historyService: HistoryService,
    private val aspectDeltaConstructor: AspectDeltaConstructor,
    private val refBookDao: ReferenceBookDao,
    private val subjectDao: SubjectDao
) {
    fun getAllHistory(): List<AspectHistory> {
        val bothFacts = historyService.allTimeline(listOf(ASPECT_CLASS, ASPECT_PROPERTY_CLASS))

        val factsByClass = bothFacts.groupBy { it.event.entityClass }
        val aspectFacts = factsByClass[ASPECT_CLASS] ?: emptyList()
        val aspectFactsByEntity = aspectFacts.groupBy { it.event.entityId }
        val propertyFacts = factsByClass[ASPECT_PROPERTY_CLASS] ?: emptyList()
        val propertyFactsBySession = propertyFacts.groupBy { it.event.sessionId }

        val propertySnapshots = propertyFacts.map { it.event.entityId to MutableSnapshot() }.toMap()

        val refBookIds = aspectFacts.flatMap { fact ->
            fact.payload.mentionedLinks(AspectField.REFERENCE_BOOK)
        }.toSet()

        val refBookNames = logTime(logger, "extracting ref book names") {
            refBookDao.find(refBookIds.toList()).groupBy {it.id}.mapValues { it.value.first().value }
        }

        val subjectIds = aspectFacts.flatMap { fact ->
            fact.payload.mentionedLinks(AspectField.SUBJECT)
        }.toSet()

        val subjectById = logTime(logger, "extracting subjects") {
                subjectDao.find(subjectIds.toList()).groupBy {it.id}.mapValues { (id, elems) ->
                val subjectVertex = elems.first()
                SubjectData(id = subjectVertex.id, name = subjectVertex.name, description = subjectVertex.description, version = subjectVertex.version)
            }
        }

        logger.info("subject by id: $subjectById")

        val events = logTime(logger, "processing aspect event groups") {
            aspectFactsByEntity.values.flatMap {  aspectFacts ->

                val snapshot = MutableSnapshot()

                var aspectDataAccumulator = AspectData(name = "")

                val versionList = listOf(AspectData(id = null, name = "")) + aspectFacts.map { aspectFact ->
                    if (!aspectFact.event.type.isDelete()) {
                        snapshot.apply(aspectFact.payload)
                        val propertyFacts = propertyFactsBySession[aspectFact.event.sessionId]
                        propertyFacts?.forEach { propertyFact ->
                            propertySnapshots[propertyFact.event.entityId]?.apply(propertyFact.payload)
                            propertySnapshots[propertyFact.event.entityId]?.data?.set("_version", propertyFact.event.version.toString())
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
                            version = propSnapshot.data["_version"]?.toInt()?:-1)
                    }

                    val refBookName = snapshot.links[AspectField.REFERENCE_BOOK]?.firstOrNull()?.let { refBookNames[it.toString()] }

                    val subject = snapshot.links[AspectField.SUBJECT]?.firstOrNull()?.let {
                        subjectById[it.toString()]
                    }

                    AspectData(id = null,
                        name = snapshot.data.getValue(AspectField.NAME.name),
                        description = snapshot.data[AspectField.DESCRIPTION.name],
                        baseType = snapshot.data[AspectField.BASE_TYPE.name],
                        domain = baseType?.let { OpenDomain(BaseType.restoreBaseType(it)).toString() },
                        measure = snapshot.data[AspectField.MEASURE.name],
                        properties = properties,
                        version = aspectFact.event.version,
                        deleted = aspectFact.event.type.isDelete(),
                        refBookName = refBookName,
                        subject = subject
                    )
                }

                return@flatMap logTime(logger, "aspect diffs creation for aspect ${aspectFacts.firstOrNull()?.event?.entityId}") {
                    versionList.zipWithNext().zip(aspectFacts)
                        .map {
                            val res = aspectDeltaConstructor.createDiff(it.first.first, it.first.second, it.second)
                            logger.info("diff res: $res")
                            res
                        }
                }
            }
        }

        return events.sortedByDescending { it.event.timestamp }
    }
}