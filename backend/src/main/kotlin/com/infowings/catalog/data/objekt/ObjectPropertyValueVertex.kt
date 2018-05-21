package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.Range
import com.infowings.catalog.data.aspect.AspectPropertyVertex
import com.infowings.catalog.data.aspect.toAspectPropertyVertex
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
import java.math.BigDecimal

fun OVertex.toObjectPropertyValueVertex() = ObjectPropertyValueVertex(this)

const val INT_TYPE_PROPERTY = "int"
const val DECIMAL_TYPE_PROPERTY = "decimal"
const val STR_TYPE_PROPERTY = "str"
const val COMPOUND_TYPE_PROPERTY = "compound"
const val RANGE_TYPE_PROPERTY = "range"
const val PRECISION_PROPERTY = "precision"
private const val TYPE_TAG_PROPERTY = "type_tag"

/* Коды значений хранятся в базе, поэтому при любых изменениях/дополнениях надо сохранять
   коды. Или править базу соответственно.
  */
enum class ScalarTypeTag(val code: Int) {
    INTEGER(1),
    STRING(2),
    RANGE(3),
    COMPOUND(4),
    DECIMAL(5),
    OBJECT(100),
    SUBJECT(101),
    DOMAIN_ELEMENT(102),
}

/*
   На уровне хранения в базе используем общий тег для того, чтобы отличать любые значения - и скалярные, и ссылки
 */
fun ObjectValue.tag() = when (this) {
    is ObjectValue.IntegerValue -> ScalarTypeTag.INTEGER
    is ObjectValue.StringValue -> ScalarTypeTag.STRING
    is ObjectValue.RangeValue -> ScalarTypeTag.RANGE
    is ObjectValue.CompoundValue -> ScalarTypeTag.COMPOUND
    is ObjectValue.DecimalValue -> ScalarTypeTag.DECIMAL
    is ObjectValue.Link -> when (this.value) {
        is LinkValueVertex.ObjectValue -> ScalarTypeTag.OBJECT
        is LinkValueVertex.SubjectValue -> ScalarTypeTag.SUBJECT
        is LinkValueVertex.DomainElementValue -> ScalarTypeTag.DOMAIN_ELEMENT
    }
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
            return intTag?.let { tagByInt[intTag] ?: throw IncorrectTypeTagException(id, it) }
        }
        set(value) {
            vertex[TYPE_TAG_PROPERTY] = value?.code
        }

    var intValue: Int?
        get() = vertex[INT_TYPE_PROPERTY]
        set(value) {
            vertex[INT_TYPE_PROPERTY] = value
        }
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
            val s: String? = vertex[RANGE_TYPE_PROPERTY]
            return s?.let {
                val parts = it.split(":").map { it.toInt() }
                return Range(parts[0], parts[1])
            }
        }
        set(value) {
            setOrRemove(RANGE_TYPE_PROPERTY, value?.let { "${value.left}:${value.right}" })
        }
    private val rangeStrict: Range
        get() = range ?: throw RangeNotDefinedException(id)

    var decimalValue: BigDecimal?
        get() = vertex[DECIMAL_TYPE_PROPERTY]
        set(value) {
            vertex[DECIMAL_TYPE_PROPERTY] = value
        }
    private val decimalValueStrict: BigDecimal
        get() = decimalValue ?: throw DecimalValueNotDefinedException(id)


    var precision: Int?
        get() = vertex[PRECISION_PROPERTY]
        set(v) = setOrRemove(PRECISION_PROPERTY, v)

    val objectProperty: ObjectPropertyVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_OBJECT_PROPERTY_EDGE).firstOrNull()
            ?.toObjectPropertyVertex()

    val aspectProperty: AspectPropertyVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_ASPECT_PROPERTY_EDGE).firstOrNull()
            ?.toAspectPropertyVertex()

    val parentValue: ObjectPropertyValueVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_OBJECT_VALUE_EDGE).firstOrNull()
            ?.toObjectPropertyValueVertex()

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

        val value: ObjectValue = when (typeTag) {
            ScalarTypeTag.OBJECT ->
                ObjectValue.Link(
                    LinkValueVertex.ObjectValue(
                        refValueObject ?: throw ObjectVertexNotDefinedException(id)
                    )
                )
            ScalarTypeTag.SUBJECT ->
                ObjectValue.Link(
                    LinkValueVertex.SubjectValue(
                        refValueSubject ?: throw SubjectVertexNotDefinedException(
                            id
                        )
                    )
                )
            ScalarTypeTag.DOMAIN_ELEMENT ->
                ObjectValue.Link(
                    LinkValueVertex.DomainElementValue(
                        refValueDomainElement ?: throw DomainElementVertexNotDefinedException(id)
                    )
                )
            ScalarTypeTag.INTEGER -> ObjectValue.IntegerValue(intValueStrict, precision)
            ScalarTypeTag.STRING -> ObjectValue.StringValue(strValueStrict)
            ScalarTypeTag.RANGE -> ObjectValue.RangeValue(rangeStrict)
            ScalarTypeTag.COMPOUND -> ObjectValue.CompoundValue(compoundValueStrict)
            ScalarTypeTag.DECIMAL -> ObjectValue.DecimalValue(decimalValueStrict)
            else ->
                throw IllegalStateException("type tag is not defined")
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

class DecimalValueNotDefinedException(id: String) :
    ObjectValueException("decimal value is not defined for value $id")

class StringValueNotDefinedException(id: String) :
    ObjectValueException("string value is not defined for value $id")

class CompoundValueNotDefinedException(id: String) :
    ObjectValueException("compound value is not defined for value $id")

class RangeNotDefinedException(id: String) :
    ObjectValueException("range is not defined for value $id")

class ObjectVertexNotDefinedException(id: String) :
    ObjectValueException("object vertex is not defined for value $id")

class SubjectVertexNotDefinedException(id: String) :
    ObjectValueException("subject vertex is not defined for value $id")

class DomainElementVertexNotDefinedException(id: String) :
    ObjectValueException("domain element vertex is not defined for value $id")

class IncorrectTypeTagException(id: String, tag: Int) :
    ObjectValueException("incorrect type tag for object value $id, tag: $tag")
