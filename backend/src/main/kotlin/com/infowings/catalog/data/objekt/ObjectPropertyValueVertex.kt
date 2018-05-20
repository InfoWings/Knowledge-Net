package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.Range
import com.infowings.catalog.common.LinkTypeGroup
import com.infowings.catalog.common.ScalarValue
import com.infowings.catalog.data.aspect.*
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.data.reference.book.ReferenceBookItemVertex
import com.infowings.catalog.data.reference.book.toReferenceBookItemVertex
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.data.subject.toSubjectVertex
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toObjectPropertyValueVertex() = ObjectPropertyValueVertex(this)

const val INT_TYPE_PROPERTY = "int_type"
const val STR_TYPE_PROPERTY = "str_type"
const val COMPOUND_TYPE_PROPERTY = "compound_type"
private const val TYPE_TAG_PROPERTY = "type_tag"

/* Коды значений хранятся в базе, поэтому при любых изменениях/дополнениях надо сохранять
   коды. Или править базу соответственно.
 */
enum class ScalarTypeTag(val code: Int) {
    INTEGER(1),
    STRING(2),
    COMPOUND(3)
}

val tagByInt: Map<Int, ScalarTypeTag> = ScalarTypeTag.values().map { it.code to it }.toMap()

class ObjectPropertyValueVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = OBJECT_PROPERTY_VALUE_CLASS

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            "range" to asStringOrEmpty(range),
            "precision" to asStringOrEmpty(precision)
        ),
        links = emptyMap()
    )

    var typeTag: ScalarTypeTag?
        get() {
            val intTag: Int? = vertex[TYPE_TAG_PROPERTY]
            return intTag ?.let { tagByInt[intTag] ?: throw IncorrectTypeTagException(id, it) }
        }
        set(value) { vertex[TYPE_TAG_PROPERTY] = value?.code }

    var intValue: Int?
        get() = vertex[INT_TYPE_PROPERTY]
        set(value) { vertex[INT_TYPE_PROPERTY] = value }
    private val intValueStrict: Int
        get() = intValue ?: throw IntValueNotDefinedException(id)

    var strValue: String?
        get() = vertex[STR_TYPE_PROPERTY]
        set(value) {
            vertex[STR_TYPE_PROPERTY] = value
        }
    private val strValueStrict: String
        get() = strValue ?: throw StringValueNotDefinedException(id)

    var compoundValue: String?
        get() = vertex[COMPOUND_TYPE_PROPERTY]
        set(value) {
            vertex[COMPOUND_TYPE_PROPERTY] = value
        }
    private val compoundValueStrict: String
        get() = compoundValue ?: throw CompoundValueNotDefinedException(id)


    private fun <T> setOrRemove(key: String, value: T?) {
        if (value != null) {
            vertex[key] = value
        } else {
            val current: T = vertex[key]
            if (current != null) {
                vertex.removeProperty<T>(key)
            }
        }
    }

    var range: Range?
        get() {
            val s: String? = vertex["range"]
            return s?.let {
                val parts = it.split(":").map { it.toInt() }
                return Range(parts[0], parts[1])
            }
        }
        set(value) { setOrRemove("range", value?.let {"${value.left}:${value.right}"}) }

    var precision: Int?
        get() = vertex["precision"]
        set(v) = setOrRemove("precision", v)

    val objectProperty: ObjectPropertyVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_OBJECT_PROPERTY_EDGE).firstOrNull()
            ?.toObjectPropertyVertex()

    val aspectProperty: AspectPropertyVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_ASPECT_PROPERTY_EDGE).firstOrNull()
            ?.toAspectPropertyVertex()

    val parentValue: ObjectPropertyValueVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_OBJECT_VALUE_EDGE).firstOrNull()
            ?.toObjectPropertyValueVertex()

    var refValueType: String?
        get() = vertex["refValueType"]
        set(value) { vertex["refValueType"] = value }

    val refValueObject: ObjectVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_OBJECT_EDGE).firstOrNull()
            ?.toObjectVertex()

    val refValueSubject: SubjectVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_SUBJECT_EDGE).firstOrNull()
            ?.toSubjectVertex()

    val refValueDomainElement: ReferenceBookItemVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_REFBOOK_ITEM_EDGE).firstOrNull()
            ?.toReferenceBookItemVertex()

    val measure: OVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_MEASURE_EDGE).firstOrNull()

    fun toObjectPropertyValue(): ObjectPropertyValue {
        val currentProperty = objectProperty ?: throw ObjectValueWithoutPropertyException(this)
        val currentAspectProperty = aspectProperty ?: throw ObjectValueWithoutAspectPropertyException(this)

        val refValueVertex: LinkValueVertex? = refValueType ?. let {
            when (it) {
                LinkTypeGroup.SUBJECT.name ->
                    LinkValueVertex.SubjectValue(refValueSubject ?: throw SubjectVertexNotDefinedException(id))
                LinkTypeGroup.OBJECT.name ->
                    LinkValueVertex.ObjectValue(refValueObject ?:  throw ObjectVertexNotDefinedException(id))
                LinkTypeGroup.DOMAIN_ELEMENT.name ->
                    LinkValueVertex.DomainElementValue(refValueDomainElement
                            ?: throw DomainElementVertexNotDefinedException(id))
                else -> throw IllegalStateException("unknown link value vertex type: $refValueType")
            }
        }

        val value = when (refValueVertex) {
            null -> {
                val simpleData: ScalarValue? = when (typeTag) {
                    ScalarTypeTag.INTEGER -> ScalarValue.IntegerValue(intValueStrict)
                    ScalarTypeTag.STRING -> ScalarValue.StringValue(strValueStrict)
                    ScalarTypeTag.COMPOUND -> ScalarValue.CompoundValue(compoundValueStrict)
                    else -> null
                }
                ObjectValue.Scalar(simpleData, range, precision)
            }
            else -> ObjectValue.Link(refValueVertex)
        }

        return ObjectPropertyValue(identity, value, currentProperty, currentAspectProperty, parentValue, measure)
    }
}

abstract class ObjectValueException(message: String) : Exception(message)
class ObjectValueWithoutPropertyException(vertex: ObjectPropertyValueVertex) :
    ObjectValueException("Object property vertex not linked for ${vertex.id} ")
class ObjectValueWithoutAspectPropertyException(vertex: ObjectPropertyValueVertex) :
    ObjectValueException("Aspect property vertex not linked for ${vertex.id} ")

class IntValueNotDefinedException(id: String) :
    ObjectValueException("int value is not defined for value $id")

class StringValueNotDefinedException(id: String) :
    ObjectValueException("string value is not defined for value $id")

class CompoundValueNotDefinedException(id: String) :
    ObjectValueException("compound value is not defined for value $id")

class ObjectVertexNotDefinedException(id: String) :
    ObjectValueException("object vertex is not defined for value $id")
class SubjectVertexNotDefinedException(id: String) :
    ObjectValueException("subject vertex is not defined for value $id")
class DomainElementVertexNotDefinedException(id: String) :
    ObjectValueException("domain element vertex is not defined for value $id")

class IncorrectTypeTagException(id: String, tag: Int) :
    ObjectValueException("incorrect type tag for object value $id, tag: $tag")
