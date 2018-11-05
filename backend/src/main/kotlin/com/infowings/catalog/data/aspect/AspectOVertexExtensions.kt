package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.history.*
import com.infowings.catalog.data.objekt.DeletableVertex
import com.infowings.catalog.data.objekt.ObjectValue
import com.infowings.catalog.data.objekt.toObjectPropertyVertex
import com.infowings.catalog.data.reference.book.ASPECT_REFERENCE_BOOK_EDGE
import com.infowings.catalog.data.reference.book.ReferenceBookItemVertex
import com.infowings.catalog.data.reference.book.toReferenceBookItemVertex
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.data.subject.toSubjectVertex
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex
import hasIncomingEdges
import java.time.Instant

fun OVertex.toAspectVertex(): AspectVertex {
    checkClass(OrientClass.ASPECT)
    return AspectVertex(this)
}

fun OVertex.toAspectPropertyVertex(): AspectPropertyVertex {
    checkClass(OrientClass.ASPECT_PROPERTY)
    return AspectPropertyVertex(this)
}

fun OVertex.isJustCreated() = this.identity.isNew


/**
 * Kotlin does not provide package-level declarations.
 * These OVertex extensions must be available for whole package and nowhere else without special methods calls.
 * by vertex means simple delegating OVertex calls to property [vertex]
 * */
class AspectVertex(private val vertex: OVertex) : HistoryAware, GuidAware, DeletableVertex, OVertex by vertex {
    override val entityClass = ASPECT_CLASS

    private val logger = loggerFor<AspectVertex>()

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            AspectField.NAME.name to asStringOrEmpty(name),
            AspectField.MEASURE.name to asStringOrEmpty(measure),
            AspectField.BASE_TYPE.name to asStringOrEmpty(baseType),
            AspectField.DESCRIPTION.name to asStringOrEmpty(description),
            AspectField.GUID.name to asStringOrEmpty(guid)
        ),
        links = mapOf(
            AspectField.PROPERTY to properties.map { it.identity },
            AspectField.SUBJECT to (subjectVertex?.let { listOf(it.identity) } ?: emptyList()),
            AspectField.REFERENCE_BOOK to (referenceBookRootVertex?.let { listOf(it.identity) } ?: emptyList())
        )
    )

    val properties: List<AspectPropertyVertex>
        get() = vertex.getVertices(ODirection.OUT, ASPECT_ASPECT_PROPERTY_EDGE).map { it.toAspectPropertyVertex() }.toList()

    val referenceBookRootVertex: ReferenceBookItemVertex?
        get() = vertex.getVertices(ODirection.OUT, ASPECT_REFERENCE_BOOK_EDGE)
            .map { it.toReferenceBookItemVertex() }
            .filterNot { it.deleted }
            .firstOrNull()

    val referenceBookRootVertexSoft: ReferenceBookItemVertex?
        get() = vertex.getVertices(ODirection.OUT, ASPECT_REFERENCE_BOOK_EDGE)
            .map { it.toReferenceBookItemVertex() }
            .firstOrNull()

    fun dropRefBookEdge() {
        vertex.getEdges(ODirection.OUT, ASPECT_REFERENCE_BOOK_EDGE).forEach { it.delete<OEdge>() }
    }

    var baseType: String?
        get() = measure?.baseType?.name ?: this["baseType"]
        set(value) {
            vertex["baseType"] = value
        }

    val baseTypeStrict: String
        get() = baseType ?: throw IllegalStateException("Aspect vertex $this does not have base type")

    var name: String
        get() = vertex[ATTR_NAME]
        set(value) {
            vertex[ATTR_NAME] = value
        }

    val measure: Measure<DecimalNumber>?
        get() = GlobalMeasureMap[measureName]

    var measureName: String?
        get() = vertex["measure"]
        set(value) {
            vertex["measure"] = value
        }

    val subject: Subject?
        get() = subjectVertex?.toSubject()

    override
    val guid: String?
        get() = guid(OrientEdge.GUID_OF_ASPECT)

    private val lastChange: Instant?
        get() {
            // это все работает медленно. Чем больше история, тем медленнее
            val maybeLastAspectUpdate: Instant? = vertex.getVertices(ODirection.OUT, HISTORY_EDGE).map { it.toHistoryEventVertex().timestamp }.max()
            val maybeLastPropertyUpdates: List<Instant?> = vertex.getVertices(ODirection.OUT, ASPECT_ASPECT_PROPERTY_EDGE).map {
                it.getVertices(ODirection.OUT, HISTORY_EDGE).map { it.toHistoryEventVertex().timestamp }.max()
            }
            return maybeLastPropertyUpdates.filterNotNull().let { lastPropertyUpdates ->
                maybeLastAspectUpdate?.let { lastPropertyUpdates.plus(it) } ?: lastPropertyUpdates
            }.max()
        }

    private val subjectVertex: SubjectVertex?
        get() {
            val subjects = vertex.getVertices(ODirection.OUT, ASPECT_SUBJECT_EDGE).toList()
            if (subjects.size > 1) {
                throw OnlyOneSubjectForAspectIsAllowed(name)
            }
            return subjects.firstOrNull()?.toSubjectVertex()
        }

    var description: String?
        get() = vertex[ATTR_DESC]
        set(value) {
            vertex[ATTR_DESC] = value
        }

    fun isLinkedBy() = hasIncomingEdges(ASPECT_ASPECT_PROPERTY_EDGE, ASPECT_OBJECT_PROPERTY_EDGE, OBJECT_VALUE_ASPECT_EDGE)

    fun existsAspectImplementation(): Boolean {
        val inPropertyWithValue = lazy {
            getVertices(ODirection.IN, ASPECT_ASPECT_PROPERTY_EDGE).any {
                it.toAspectPropertyVertex().existsAspectPropertyImplementation()
            }
        }

        val hasObjectPropertyWithValue = lazy {
            getVertices(ODirection.IN, ASPECT_OBJECT_PROPERTY_EDGE).any {
                it.toObjectPropertyVertex().values.any {
                    val objPropertyValue = it.toObjectPropertyValue()
                    objPropertyValue.aspectProperty == null && objPropertyValue.value != ObjectValue.NullValue
                }
            }
        }

        return inPropertyWithValue.value || hasObjectPropertyWithValue.value
    }

    override fun equals(other: Any?): Boolean {
        return vertex == other
    }

    override fun hashCode(): Int {
        return vertex.hashCode()
    }

    override fun toString(): String =
        "AspectVertex[id=${this.id}, name=${this.name}]"

    private fun toAspectOnlyData(): AspectData {
        val baseTypeObj = baseType?.let { BaseType.restoreBaseType(it) }
        val subjectData = subject?.toSubjectData()
        val refBookValue = referenceBookRootVertex?.value
        val lastChange = logTime(logger, "extracting last change") { lastChange }

        return AspectData(
            id = id,
            name = name,
            measure = measureName,
            domain = baseTypeObj?.let { OpenDomain(it).toString() },
            baseType = baseType,
            properties = emptyList(),
            version = version,
            subject = subjectData,
            deleted = deleted,
            description = description,
            lastChangeTimestamp = lastChange?.epochSecond,
            refBookName = refBookValue,
            guid = guid
        )
    }

    private fun toAspectLocalData(): AspectData {
        val baseTypeObj = baseType?.let { BaseType.restoreBaseType(it) }

        return AspectData(
            id = id,
            name = name,
            measure = measureName,
            domain = baseTypeObj?.let { OpenDomain(it).toString() },
            baseType = baseType,
            properties = emptyList(),
            version = version,
            subject = null,
            deleted = deleted,
            description = description,
            lastChangeTimestamp = null,
            refBookName = null
        )
    }

    // медленный вызов, в первую очередь за счет lastChange, во вторую -  за счет ссылок на субъект и имя справочника
    // может быть приемлемым при работе с одним аспектом
    fun toAspectData(): AspectData = toAspectOnlyData().copy(properties = properties.map { it.toAspectPropertyData() }, guid = guid ?: "???")

    fun toAspectData(properties: Map<String, AspectPropertyData>, guids: Map<String, String>, details: AspectDaoDetails): AspectData {
        val propertiesData = details.propertyIds.mapNotNull {
            val propertyId = it.toString()
            val data: AspectPropertyData? = properties[propertyId]
            data ?: logger.warn("Not found aspect property with id $propertyId. Aspect id: $id")
            data
        }
        val data = logTime(logger, "get aspect only data") { toAspectLocalData() }
        return data.copy(
            properties = propertiesData, subject = details.subject, refBookName = details.refBookName,
            lastChangeTimestamp = details.lastChange.epochSecond, guid = guids[id]
        )
    }
}

class OnlyOneSubjectForAspectIsAllowed(name: String) : Exception("Too many subject for aspect '$name'")

class AspectPropertyVertex(private val vertex: OVertex) : HistoryAware, GuidAware, OVertex by vertex {
    override val entityClass = ASPECT_PROPERTY_CLASS

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            AspectPropertyField.NAME.name to asStringOrEmpty(name),
            AspectPropertyField.ASPECT.name to asStringOrEmpty(aspect),
            AspectPropertyField.CARDINALITY.name to asStringOrEmpty(cardinality),
            AspectPropertyField.DESCRIPTION.name to asStringOrEmpty(description),
            AspectPropertyField.GUID.name to asStringOrEmpty(guid)
        ),
        links = emptyMap()
    )

    fun toAspectPropertyData(): AspectPropertyData = AspectPropertyData(id, name, aspect, associatedAspect.guid ?: "???", cardinality, description, version, deleted, guid)

    var name: String?
        get() = vertex["name"]
        set(value) {
            vertex["name"] = value
        }

    var nameWithAspect: String?
        get() = vertex["name_with_aspect"]
        set(value) {
            vertex["name_with_aspect"] = value
        }

    var aspect: String
        get() = vertex["aspectId"]
        set(value) {
            vertex["aspectId"] = value
        }

    var cardinality: String
        get() = vertex["cardinality"]
        set(value) {
            vertex["cardinality"] = value
        }

    var deleted: Boolean
        get() = vertex["deleted"] ?: false
        set(value) {
            vertex["deleted"] = value
        }

    var description: String?
        get() = vertex[ATTR_DESC]
        set(value) {
            vertex[ATTR_DESC] = value
        }

    override
    val guid: String?
        get() = guid(OrientEdge.GUID_OF_ASPECT_PROPERTY)

    val associatedAspect: AspectVertex
        get() = vertex.getVertices(ODirection.OUT, ASPECT_ASPECT_PROPERTY_EDGE).single().toAspectVertex()

    val parentAspect: AspectVertex
        get() = vertex.getVertices(ODirection.IN, ASPECT_ASPECT_PROPERTY_EDGE).first().toAspectVertex()

    fun isLinkedBy() = hasIncomingEdges(OBJECT_VALUE_ASPECT_PROPERTY_EDGE, OBJECT_VALUE_REF_ASPECT_PROPERTY_EDGE)

    fun existsAspectPropertyImplementation(): Boolean = isLinkedBy()

    override fun equals(other: Any?): Boolean {
        return vertex == other
    }

    override fun hashCode(): Int {
        return vertex.hashCode()
    }
}