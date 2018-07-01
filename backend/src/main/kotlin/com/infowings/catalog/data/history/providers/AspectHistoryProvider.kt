package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.aspect.OpenDomain
import com.infowings.catalog.data.history.*
import com.infowings.catalog.data.reference.book.ReferenceBookDao
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import java.util.concurrent.CopyOnWriteArrayList

private val logger = loggerFor<AspectHistoryProvider>()

private data class DataWithSnapshot(val data: AspectData, val snapshot: Snapshot)

private val changeNamesConvert = mapOf(
    AspectField.NAME.name to "Name",
    AspectField.BASE_TYPE.name to "Base type",
    AspectField.DESCRIPTION.name to "Description",
    AspectField.MEASURE.name to "Measure"
    )

class AspectHistoryProvider(
    private val historyService: HistoryService,
    private val aspectDeltaConstructor: AspectDeltaConstructor,
    private val refBookDao: ReferenceBookDao,
    private val subjectDao: SubjectDao,
    private val aspectDao: AspectDaoService,
    private val db: OrientDatabase
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

        val aspectIds = propertyFacts.map { fact ->
            fact.payload.data[AspectPropertyField.ASPECT.name]
        }.filterNotNull().toSet()

        logger.info("mentioned aspectIds: " + aspectIds)

        val aspectsById = logTime(logger, "obtaining aspects") {
            val vertices = aspectDao.findAspectsByIdsStr(aspectIds.toList())
            transaction(db) {
                vertices.map { it.id to it.toAspectDataLazy()
                    .toAspectData(subjectById, emptyMap()).copy(lastChangeTimestamp =
                    aspectFactsByEntity[it.id]?.lastOrNull()?.event?.timestamp?.let { it / 1000 }?:-1 ) }
            }
        }.toMap()

        logger.info("aspects by Id: " + aspectsById)

        val events = logTime(logger, "processing aspect event groups") {
            aspectFactsByEntity.values.flatMap {  aspectFacts ->

                val snapshot = MutableSnapshot()

                val versionList = listOf(DataWithSnapshot(AspectData(id = null, name = ""), snapshot.toSnapshot())) + aspectFacts.map { aspectFact ->
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

                    DataWithSnapshot(AspectData(id = null,
                        name = snapshot.data.getValue(AspectField.NAME.name),
                        description = snapshot.data[AspectField.DESCRIPTION.name],
                        baseType = snapshot.data[AspectField.BASE_TYPE.name],
                        domain = baseType?.let { OpenDomain(BaseType.restoreBaseType(it)).toString() },
                        measure = snapshot.data[AspectField.MEASURE.name],
                        properties = properties,
                        version = aspectFact.event.version ,
                        deleted = aspectFact.event.type.isDelete(),
                        refBookName = refBookName,
                        subject = subject
                    ), snapshot.toSnapshot())
                }

                val res = logTime(logger, "aspect diffs creation for aspect ${aspectFacts.firstOrNull()?.event?.entityId}") {
                    versionList.zipWithNext().zip(aspectFacts)
                        .map {
                            val res: AspectHistory = aspectDeltaConstructor.createDiff(it.first.first.data, it.first.second.data, it.second)
                            logger.info("diff res: $res")

                            val aspectFact = it.second
                            val before = it.first.first
                            val after = it.first.second

                            val deltas = aspectFact.payload.data.map {
                                val emptyPlaceholder = if (it.key == AspectField.NAME.name) "" else null
                                FieldDelta(changeNamesConvert.getOrDefault(it.key, it.key),
                                    before.snapshot.data[it.key]?:emptyPlaceholder,
                                    if (aspectFact.event.type.isDelete()) null else after.snapshot.data[it.key])
                            }

                            val res2 = AspectHistory(aspectFact.event, after.data.name, after.data.deleted,
                                AspectDataView(after.data, after.data.properties.mapNotNull {
                                    aspectsById[it.aspectId]
                                }), deltas)

                            logger.info("res.fdata: ${res.fullData}")
                            logger.info("res2.fdata: ${res2.fullData}")
                            logger.info("res.changes: ${res.changes}")
                            logger.info("res2.changes: ${res2.changes}")
                            logger.info("7 res.changes==res2.changes: ${res.changes == res2.changes}")
                            logger.info("7 res==res2: ${res==res2}")

                            res
                        }
                }

                return@flatMap res
            }
        }

        return events.sortedByDescending { it.event.timestamp }
    }
}