package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.aspect.OpenDomain
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.MutableSnapshot
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.linksOfType
import com.infowings.catalog.data.reference.book.ReferenceBookDao
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.ASPECT_PROPERTY_CLASS
import com.infowings.catalog.storage.id

private val logger = loggerFor<AspectHistoryProvider>()

private data class DataWithSnapshot(val data: AspectData, val snapshot: Snapshot, val propSnapshots: Map<String, Snapshot>)

private val changeNamesConvert = mapOf(
    AspectField.NAME.name to "Name",
    AspectField.BASE_TYPE.name to "Base type",
    AspectField.DESCRIPTION.name to "Description",
    AspectField.MEASURE.name to "Measure",
    AspectField.SUBJECT to "Subject",
    AspectField.REFERENCE_BOOK to "Reference book"
)

private val removedSubject = SubjectData(id = "", name = "Subject removed", description = "")

class AspectHistoryProvider(
    private val historyService: HistoryService,
    private val aspectDeltaConstructor: AspectDeltaConstructor,
    private val refBookDao: ReferenceBookDao,
    private val subjectDao: SubjectDao,
    private val aspectService: AspectService
) {
    fun getAllHistory(): List<AspectHistory> {
        val bothFacts = historyService.allTimeline(listOf(ASPECT_CLASS, ASPECT_PROPERTY_CLASS))

        val factsByClass = bothFacts.groupBy { it.event.entityClass }
        val aspectFacts = factsByClass[ASPECT_CLASS].orEmpty()
        val aspectFactsByEntity = aspectFacts.groupBy { it.event.entityId }
        val propertyFacts = factsByClass[ASPECT_PROPERTY_CLASS].orEmpty()
        val propertyFactsBySession = propertyFacts.groupBy { it.event.sessionId }
        val propertySnapshots = propertyFacts.map { it.event.entityId to MutableSnapshot() }.toMap()

        val refBookIds = aspectFacts.linksOfType(AspectField.REFERENCE_BOOK)

        val refBookNames = logTime(logger, "extracting ref book names") {
            refBookDao.find(refBookIds.toList()).groupBy {it.id}.mapValues { it.value.first().value }
        }

        val subjectIds = aspectFacts.linksOfType(AspectField.SUBJECT)

        val subjectById = logTime(logger, "extracting subjects") {
            val found = subjectDao.find(subjectIds.toList())
            found.groupBy {it.id}.mapValues { (_, elems) ->
                val subjectVertex = elems.first()
                SubjectData(id = subjectVertex.id, name = subjectVertex.name, description = subjectVertex.description, version = subjectVertex.version)
            }
        }

        val aspectIds = propertyFacts.mapNotNull { fact ->
            fact.payload.data[AspectPropertyField.ASPECT.name]
        }.toSet()

        val aspectsData = aspectService.getAspectsWithDeleted(aspectIds.toList())
        val aspectsById: Map<String, AspectData> = aspectsData.map { it.id!! to it }.toMap()

        val events = logTime(logger, "processing aspect event groups") {
            aspectFactsByEntity.values.flatMap {  aspectFacts ->
                val snapshot = MutableSnapshot()

                val versionList = listOf(DataWithSnapshot(AspectData(id = null, name = ""), snapshot.toSnapshot(), emptyMap())) + aspectFacts.map { aspectFact ->
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
                            name = propSnapshot.dataOrEmpty(AspectPropertyField.NAME.name),
                            aspectId = propSnapshot.dataOrEmpty(AspectPropertyField.ASPECT.name),
                            cardinality = propSnapshot.dataOrEmpty(AspectPropertyField.CARDINALITY.name),
                            description = propSnapshot.data[AspectPropertyField.DESCRIPTION.name],
                            version = propSnapshot.data["_version"]?.toInt()?:-1)
                    }

                    val refBookName = snapshot.resolvedLink(AspectField.REFERENCE_BOOK) {
                        refBookNames[it]?: "???"
                    }
                    val subject = snapshot.resolvedLink(AspectField.SUBJECT) {
                        subjectById[it] ?: removedSubject
                    }

                    val data = AspectData(id = null,
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
                    )

                    val propertySnapshotsById = properties.map {
                        val propId = it.id
                        val propSnapshot = propertySnapshots[propId] ?: MutableSnapshot()
                        propId to propSnapshot.toSnapshot()
                    }.toMap()

                    DataWithSnapshot(data, snapshot.toSnapshot(), propertySnapshotsById)
                }

                val res = logTime(logger, "aspect diffs creation for aspect ${aspectFacts.firstOrNull()?.event?.entityId}") {
                    versionList.zipWithNext().zip(aspectFacts)
                        .map { (versionsPair, aspectFact) ->
                            val res: AspectHistory = aspectDeltaConstructor.createDiff(versionsPair.first.data, versionsPair.second.data, aspectFact)

                            val (before, after) = versionsPair

                            val propertyFactsByType = (propertyFactsBySession[aspectFact.event.sessionId]?: emptyList()).groupBy { it.event.type }

                            val createPropertyDeltas = (propertyFactsByType[EventType.CREATE] ?: emptyList()).map { propertyFact ->
                                val name = propertyFact.payload.data[AspectPropertyField.NAME.name]
                                val cardinality = propertyFact.payload.data[AspectPropertyField.CARDINALITY.name]?.let {
                                    PropertyCardinality.valueOf(it).label
                                }
                                val aspectId = propertyFact.payload.data[AspectPropertyField.ASPECT.name] ?: ""
                                FieldDelta("Property ${name ?: ""}", null, "${name ?: ""} ${aspectsById[aspectId]?.name?:"'Aspect removed'"} : [$cardinality]")
                            }

                            val updatePropertyDeltas = (propertyFactsByType[EventType.UPDATE] ?: emptyList()).filterNot {
                                it.payload.data.isEmpty() && it.payload.addedLinks.isEmpty() && it.payload.removedLinks.isEmpty()
                            }.map { propertyFact ->
                                val name = propertyFact.payload.data[AspectPropertyField.NAME.name]
                                val prevSnapshot: Snapshot = before.propSnapshots[propertyFact.event.entityId] ?: Snapshot()
                                logger.info("previous previous snapshot: $prevSnapshot")
                                val prevName =  prevSnapshot.data.get(AspectPropertyField.NAME.name) ?: ""
                                val prevCardinality =  prevSnapshot.data.get(AspectPropertyField.CARDINALITY.name)?.let {
                                    PropertyCardinality.valueOf(it).label
                                }
                                val cardinality = propertyFact.payload.data[AspectPropertyField.CARDINALITY.name]?.let {
                                    PropertyCardinality.valueOf(it).label
                                }
                                val aspectId = prevSnapshot.data.get(AspectPropertyField.ASPECT.name) ?: ""
                                logger.info("update property fact for aspect ${aspectFact.event.entityId}: $propertyFact")
                                FieldDelta("Property ${name ?: ""}",
                                    "${prevName ?: ""} ${aspectsById[aspectId]?.name} : [$prevCardinality]",
                                    "${name ?: ""} ${aspectsById[aspectId]?.name} : [$cardinality]")
                            }

                            val deletePropertyDeltas = ((propertyFactsByType[EventType.DELETE] ?: emptyList()) +
                                    ((propertyFactsByType[EventType.SOFT_DELETE] ?: emptyList())))
                                .map { propertyFact ->
                                    val name = propertyFact.payload.data[AspectPropertyField.NAME.name]
                                    val prevSnapshot: Snapshot = before.propSnapshots[propertyFact.event.entityId] ?: Snapshot()
                                    logger.info("previous previous snapshot: $prevSnapshot")
                                    val prevName =  prevSnapshot.data.get(AspectPropertyField.NAME.name) ?: ""
                                    val prevCardinality =  prevSnapshot.data.get(AspectPropertyField.CARDINALITY.name)?.let {
                                        PropertyCardinality.valueOf(it).label
                                    }
                                    val aspectId = prevSnapshot.data.get(AspectPropertyField.ASPECT.name) ?: ""
                                    logger.info("delete property fact for aspect ${aspectFact.event.entityId}: $propertyFact")
                                    FieldDelta("Property ${name ?: " "}",
                                            "$prevName ${aspectsById[aspectId]?.name?:"'Aspect removed'"} : [$prevCardinality]",
                                            null)
                            }

                            val replacedLinks = aspectFact.payload.addedLinks.keys.intersect(aspectFact.payload.removedLinks.keys)
                            logger.info("replaced: $replacedLinks")
                            val replaceDeltas = replacedLinks.map {
                                logger.info("$it: ${aspectFact.payload.removedLinks[it]} -> ${aspectFact.payload.addedLinks[it]}")
                                val beforeId = aspectFact.payload.removedLinks[it]?.first()?.toString()
                                val afterId = aspectFact.payload.addedLinks[it]?.first()?.toString()
                                val key = it
                                val beforeName = beforeId?.let {
                                    when (key) {
                                        AspectField.SUBJECT -> subjectById[beforeId]?.name?:"Subject removed"
                                        AspectField.REFERENCE_BOOK -> refBookNames[afterId]
                                        else -> null
                                    }
                                } ?: "???"
                                val afterName = afterId?.let {
                                    when (key) {
                                        AspectField.SUBJECT -> subjectById[afterId]?.name?:"Subject removed"
                                        AspectField.REFERENCE_BOOK -> refBookNames[afterId]
                                        else -> null
                                    }
                                } ?: "???"

                                logger.info("replaced before: $beforeId, $beforeName")
                                logger.info("replaced after: $afterId, $afterName")

                                FieldDelta(
                                    changeNamesConvert.getOrDefault(it, it),
                                    beforeName,
                                    afterName
                                )
                            }

                            val deltas = aspectFact.payload.data.map {
                                val emptyPlaceholder = if (it.key == AspectField.NAME.name) "" else null
                                FieldDelta(changeNamesConvert.getOrDefault(it.key, it.key),
                                    before.snapshot.data[it.key]?:emptyPlaceholder,
                                    if (aspectFact.event.type.isDelete()) null else after.snapshot.data[it.key])
                            } + replaceDeltas + aspectFact.payload.addedLinks.mapNotNull {
                                if (it.key != AspectField.PROPERTY && !replacedLinks.contains(it.key) ) {
                                    logger.info("key: ${it.key}")
                                    logger.info("before links: " + before.snapshot.links)
                                    logger.info("after links: " + after.snapshot.links)
                                    logger.info("before data: " + before.snapshot.data)
                                    logger.info("after data: " + after.snapshot.data)
                                    val afterId = after.snapshot.links[it.key]?.first()?.toString()
                                    val key = it.key
                                    logger.info("afterId: " + afterId)
                                    val afterName = afterId?.let {
                                        when (key) {
                                            AspectField.SUBJECT -> subjectById[afterId]?.name?:"Subject removed"
                                            AspectField.REFERENCE_BOOK -> refBookNames[afterId]
                                            else -> null
                                        }
                                    } ?: "???"

                                    FieldDelta(
                                        changeNamesConvert.getOrDefault(it.key, it.key),
                                        before.snapshot.links[it.key]?.first()?.toString(),
                                        afterName
                                    )
                                } else null
                            } + aspectFact.payload.removedLinks.mapNotNull {
                                if (it.key != AspectField.PROPERTY && !replacedLinks.contains(it.key)) {
                                    logger.info("r key: ${it.key}")
                                    logger.info("r before links: " + before.snapshot.links)
                                    logger.info("r after links: " + after.snapshot.links)
                                    logger.info("r before data: " + before.snapshot.data)
                                    logger.info("r after data: " + after.snapshot.data)
                                    val beforeId = before.snapshot.links[it.key]?.first()?.toString()
                                    val key = it.key
                                    logger.info("r beforeId: " + beforeId)
                                    val beforeName = beforeId?.let {
                                        when (key) {
                                            AspectField.SUBJECT -> subjectById[beforeId]?.name
                                            AspectField.REFERENCE_BOOK -> refBookNames[beforeId]
                                            else -> null
                                        }
                                    } ?: "???"
                                    val afterId = after.snapshot.links[it.key]?.first()?.toString()
                                    logger.info("afterId: " + afterId)
                                    val afterName = afterId?.let {
                                        when (key) {
                                            AspectField.SUBJECT -> subjectById[afterId]?.name
                                            AspectField.REFERENCE_BOOK -> refBookNames[afterId]
                                            else -> null
                                        }
                                    } ?: "???"

                                    FieldDelta(
                                        changeNamesConvert.getOrDefault(it.key, it.key),
                                        beforeName,
                                        null
                                    )
                                } else null
                            } + updatePropertyDeltas + deletePropertyDeltas + createPropertyDeltas


                            val res2 = AspectHistory(aspectFact.event, after.data.name, after.data.deleted,
                                AspectDataView(after.data, after.data.properties.mapNotNull {
                                    aspectsById[it.aspectId] ?: AspectData(it.aspectId, "'Aspect removed'", null)
                                }), if (aspectFact.event.type.isDelete()) deltas.filterNot { it.fieldName in setOf("Subject", "Reference book") } else deltas)

                            logger.info("35 res==res2: ${res==res2}")

                            res
                        }
                }

                return@flatMap res
            }
        }

        return events.sortedByDescending { it.event.timestamp }
    }
}