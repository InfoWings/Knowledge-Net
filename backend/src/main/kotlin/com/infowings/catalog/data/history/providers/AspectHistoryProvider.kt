package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectDaoDetails
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.aspect.OpenDomain
import com.infowings.catalog.data.history.*
import com.infowings.catalog.data.reference.book.ReferenceBookDao
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORecordId
import java.util.concurrent.CopyOnWriteArrayList

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
            val found = subjectDao.find(subjectIds.toList())
            found.groupBy {it.id}.mapValues { (_, elems) ->
                val subjectVertex = elems.first()
                SubjectData(id = subjectVertex.id, name = subjectVertex.name, description = subjectVertex.description, version = subjectVertex.version)
            }
        }

        val aspectIdsFromProps = propertyFacts.map { fact ->
            fact.payload.data[AspectPropertyField.ASPECT.name]
        }.toSet()

        logger.info("aspect ids from prop: $aspectIdsFromProps")

        val validAspectVerts = aspectDao.getAspectsWithDeleted(aspectIdsFromProps.map { ORecordId(it)})
        val validAspectIds = validAspectVerts.map {it.identity}

        logger.info("valid aspect ids: $validAspectIds")

        val detailsById: Map<String, AspectDaoDetails> = aspectDao.getDetails(validAspectIds)

        val props = aspectDao.getProperties(validAspectIds).map {
            it.toAspectPropertyData()
        }

        val propsById = props.groupBy { it.id }.mapValues { it.value.first() }

        logger.info("details by id: $detailsById")
        logger.info("props by id: $propsById")

        val aspectsData = logTime(logger, "filling valid aspects using details") {
            validAspectVerts.mapNotNull { aspectVertex ->
                logTime(logger, "convert to aspect data") {
                    val id = aspectVertex.id
                    val details = detailsById[id]
                    val data = details?.let {
                        aspectVertex.toAspectData(propsById, details)
                    }
                    data ?: logger.warn("nothing found for aspect id $id")
                    data
                }
            }
        }

        val aspectsById2: Map<String, AspectData> = aspectsData.map { it.id!! to it }.toMap()

        val aspectIds = propertyFacts.map { fact ->
            fact.payload.data[AspectPropertyField.ASPECT.name]
        }.filterNotNull().toSet()

        val aspectsById = logTime(logger, "obtaining aspects") {
            val vertices = aspectDao.findAspectsByIdsStr(aspectIds.toList())
            transaction(db) {
                vertices.map { it.id to it.toAspectDataLazy()
                    .toAspectData(subjectById, refBookNames).copy(lastChangeTimestamp =
                    aspectFactsByEntity[it.id]?.lastOrNull()?.event?.timestamp?.let { it / 1000 }?:-1 ) }
            }
        }.toMap()

        logger.info("same aspects by Id keys: ${aspectsById.keys == aspectsById2.keys}")

        logger.info("aspects by Id: " + aspectsById)
        logger.info("aspects by Id-2: " + aspectsById2)

        aspectsById.keys.forEach {
            logger.info("id: $it")
            logger.info("aspect-1: ${aspectsById[it]}")
            logger.info("aspect-2: ${aspectsById2[it]}")
            logger.info("same aspect: ${aspectsById[it] == aspectsById2[it]?.copy(properties = emptyList())}")
        }

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
                            name = propSnapshot.data[AspectPropertyField.NAME.name] ?: "",
                            aspectId = propSnapshot.data[AspectPropertyField.ASPECT.name] ?: "",
                            cardinality = propSnapshot.data[AspectPropertyField.CARDINALITY.name] ?: "",
                            description = propSnapshot.data[AspectPropertyField.DESCRIPTION.name],
                            version = propSnapshot.data["_version"]?.toInt()?:-1)
                    }

                    val refBookName = snapshot.links[AspectField.REFERENCE_BOOK]?.firstOrNull()?.let { refBookNames[it.toString()]?: "???" }

                    val subject = snapshot.links[AspectField.SUBJECT]?.firstOrNull()?.let {
                        subjectById[it.toString()] ?: SubjectData(id = "", name = "Subject removed", description = "")
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
                    ), snapshot.toSnapshot(), properties.map {
                        val propId = it.id
                        val propSnapshot = propertySnapshots[propId] ?: MutableSnapshot()
                        propId to propSnapshot.toSnapshot()
                    }.toMap())
                }

                val res = logTime(logger, "aspect diffs creation for aspect ${aspectFacts.firstOrNull()?.event?.entityId}") {
                    versionList.zipWithNext().zip(aspectFacts)
                        .map {
                            val res: AspectHistory = aspectDeltaConstructor.createDiff(it.first.first.data, it.first.second.data, it.second)
                            logger.info("diff res: $res")

                            val aspectFact = it.second
                            val before = it.first.first
                            val after = it.first.second

                            logger.info("aspect fact: $aspectFact")
                            logger.info("before data: ${before.data}")
                            logger.info("after data: ${after.data}")

                            val propertyFactsByType = (propertyFactsBySession[aspectFact.event.sessionId]?: emptyList()).groupBy { it.event.type }

                            val createPropertyDeltas = (propertyFactsByType[EventType.CREATE] ?: emptyList()).map { propertyFact ->
                                val name = propertyFact.payload.data[AspectPropertyField.NAME.name]
                                val cardinality = propertyFact.payload.data[AspectPropertyField.CARDINALITY.name]?.let {
                                    PropertyCardinality.valueOf(it).label
                                }
                                val aspectId = propertyFact.payload.data[AspectPropertyField.ASPECT.name] ?: ""
                                logger.info("create property fact for aspect ${aspectFact.event.entityId}: $propertyFact")
                                FieldDelta("Property ${name ?: ""}", null, "${name ?: ""} ${aspectsById2[aspectId]?.name?:"'Aspect removed'"} : [$cardinality]")
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
                                    "${prevName ?: ""} ${aspectsById2[aspectId]?.name} : [$prevCardinality]",
                                    "${name ?: ""} ${aspectsById2[aspectId]?.name} : [$cardinality]")
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
                                            "$prevName ${aspectsById2[aspectId]?.name?:"'Aspect removed'"} : [$prevCardinality]",
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
                                    aspectsById2[it.aspectId] ?: AspectData(it.aspectId, "'Aspect removed'", null)
                                }), if (aspectFact.event.type.isDelete()) deltas.filterNot { it.fieldName in setOf("Subject", "Reference book") } else deltas)

                            logger.info("res.fdata1: ${res.fullData.aspectData}")
                            logger.info("res2.fdata1: ${res2.fullData.aspectData}")
                            logger.info("32 res.fdata1==res2.fdata1: ${res.fullData.aspectData == res2.fullData.aspectData}")
                            logger.info("res.fdata2: ${res.fullData.related}")
                            logger.info("res2.fdat2: ${res2.fullData.related}")
                            logger.info("32 res.fdata2==res2.fdata2: ${res.fullData.related == res2.fullData.related}")
                            logger.info("res.changes: ${res.changes}")
                            logger.info("res2.changes: ${res2.changes}")
                            logger.info("32 res.changes==res2.changes: ${res.changes == res2.changes}")
                            logger.info("32 res==res2: ${res==res2}")

                            res
                        }
                }

                return@flatMap res
            }
        }

        return events.sortedByDescending { it.event.timestamp }
    }
}