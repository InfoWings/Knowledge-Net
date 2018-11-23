package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.Range
import com.infowings.catalog.common.RangeFlagConstants
import com.infowings.catalog.data.aspect.AspectPropertyVertex
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.aspect.toAspectPropertyVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asString
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.data.reference.book.ReferenceBookItemVertex
import com.infowings.catalog.data.reference.book.toReferenceBookItemVertex
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.data.subject.toSubjectVertex
import com.infowings.catalog.data.toMeasure
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex
import java.math.BigDecimal

fun OVertex.toObjectPropertyValueVertex(): ObjectPropertyValueVertex {
    checkClass(OrientClass.OBJECT_VALUE)
    return ObjectPropertyValueVertex(this)
}

const val INT_TYPE_PROPERTY = "int"
const val INT_TYPE_UPB_PROPERTY = "intUpb"
const val DECIMAL_TYPE_PROPERTY = "decimal"
const val DECIMAL_TYPE_UPB_PROPERTY = "decimalUpb"
const val STR_TYPE_PROPERTY = "str"
const val RANGE_TYPE_PROPERTY = "range"
const val PRECISION_PROPERTY = "precision"
const val BOOL_TYPE_PROPERTY = "bool"
const val RANGE_LEFT_INF_PROPERTY = "leftInf"
const val RANGE_RIGHT_INF_PROPERTY = "rightInf"
private const val TYPE_TAG_PROPERTY = "type_tag"

/* Коды значений хранятся в базе, поэтому при любых изменениях/дополнениях надо сохранять
   коды. Или править базу соответственно.
  */
enum class ScalarTypeTag(val code: Int) {
    INTEGER(1),
    STRING(2),
    RANGE(3),
    DECIMAL(4),
    NULL(5),
    BOOLEAN(6),
    OBJECT(100),
    SUBJECT(101),
    DOMAIN_ELEMENT(102),
    ASPECT(103),
    ASPECT_PROPERTY(104),
    OBJECT_PROPERTY(105),
    OBJECT_VALUE(106),
    REF_BOOK_ITEM(107),
}

/*
   На уровне хранения в базе используем общий тег для того, чтобы отличать любые значения - и скалярные, и ссылки
 */
fun ObjectValue.tag() = when (this) {
    is ObjectValue.BooleanValue -> ScalarTypeTag.BOOLEAN
    is ObjectValue.IntegerValue -> ScalarTypeTag.INTEGER
    is ObjectValue.StringValue -> ScalarTypeTag.STRING
    is ObjectValue.RangeValue -> ScalarTypeTag.RANGE
    is ObjectValue.DecimalValue -> ScalarTypeTag.DECIMAL
    is ObjectValue.NullValue -> ScalarTypeTag.NULL
    is ObjectValue.Link -> when (this.value) {
        is LinkValueVertex.Object -> ScalarTypeTag.OBJECT
        is LinkValueVertex.ObjectProperty -> ScalarTypeTag.OBJECT_PROPERTY
        is LinkValueVertex.ObjectValue -> ScalarTypeTag.OBJECT_VALUE
        is LinkValueVertex.Subject -> ScalarTypeTag.SUBJECT
        is LinkValueVertex.DomainElement -> ScalarTypeTag.DOMAIN_ELEMENT
        is LinkValueVertex.RefBookItem -> ScalarTypeTag.REF_BOOK_ITEM
        is LinkValueVertex.Aspect -> ScalarTypeTag.ASPECT
        is LinkValueVertex.AspectProperty -> ScalarTypeTag.ASPECT_PROPERTY
    }
}

val tagByInt: Map<Int, ScalarTypeTag> = ScalarTypeTag.values().map { it.code to it }.toMap()

enum class ObjectValueField(val extName: String) {
    DESCRIPTION("description"),
    TYPE_TAG("typeTag"),
    RANGE("range"),
    PRECISION("precision"),
    INT_VALUE("intValue"),
    INT_UPB("intUpb"),
    STR_VALUE("strValue"),
    DECIMAL_VALUE("decimalValue"),
    DECIMAL_UPB("decimalUpb"),
    GUID("guid"),

    LINK_OBJECT_PROPERTY("objectProperty"),
    LINK_ASPECT_PROPERTY("aspectProperty"),
    LINK_REF_OBJECT("refValueObject"),
    LINK_REF_OBJECT_PROPERTY("refValueObjectProperty"),
    LINK_REF_OBJECT_VALUE("refValueObjectValue"),
    LINK_REF_ASPECT("refValueAspect"),
    LINK_REF_ASPECT_PROPERTY("refValueAspectProperty"),
    LINK_REF_SUBJECT("refValueSubject"),
    LINK_REF_DOMAIN_ELEMENT("refValueDomainElement"),
    LINK_MEASURE("measure"),
    LINK_PARENT_VALUE("parentValue"),
    LINK_CHILDREN("children"),
}

class ObjectPropertyValueVertex(private val vertex: OVertex) : HistoryAware, DeletableVertex, OVertex by vertex {
    override val entityClass = OBJECT_PROPERTY_VALUE_CLASS

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            ObjectValueField.DESCRIPTION.extName to asStringOrEmpty(description),
            ObjectValueField.TYPE_TAG.extName to asStringOrEmpty(typeTag),
            ObjectValueField.RANGE.extName to range?.asString().orEmpty(),
            ObjectValueField.PRECISION.extName to asStringOrEmpty(precision),
            ObjectValueField.INT_VALUE.extName to asStringOrEmpty(intValue),
            ObjectValueField.INT_UPB.extName to asStringOrEmpty(intUpb),
            ObjectValueField.STR_VALUE.extName to asStringOrEmpty(strValue),
            ObjectValueField.DECIMAL_VALUE.extName to asStringOrEmpty(decimalValue),
            ObjectValueField.DECIMAL_UPB.extName to asStringOrEmpty(decimalUpb),
            ObjectValueField.GUID.extName to asStringOrEmpty(guid)
        ),
        links = mapOf(
            ObjectValueField.LINK_OBJECT_PROPERTY.extName to listOfNotNull(objectProperty?.identity),
            ObjectValueField.LINK_ASPECT_PROPERTY.extName to listOfNotNull(aspectProperty?.identity),
            ObjectValueField.LINK_REF_OBJECT.extName to listOfNotNull(refValueObject?.identity),
            ObjectValueField.LINK_REF_OBJECT_PROPERTY.extName to listOfNotNull(refValueObjectProperty?.identity),
            ObjectValueField.LINK_REF_OBJECT_VALUE.extName to listOfNotNull(refValueObjectValue?.identity),
            ObjectValueField.LINK_REF_ASPECT.extName to listOfNotNull(refValueAspect?.identity),
            ObjectValueField.LINK_REF_ASPECT_PROPERTY.extName to listOfNotNull(refValueAspectProperty?.identity),
            ObjectValueField.LINK_REF_SUBJECT.extName to listOfNotNull(refValueSubject?.identity),
            ObjectValueField.LINK_REF_DOMAIN_ELEMENT.extName to listOfNotNull(refValueDomainElement?.identity),
            ObjectValueField.LINK_MEASURE.extName to listOfNotNull(measure?.identity),
            ObjectValueField.LINK_PARENT_VALUE.extName to listOfNotNull(parentValue?.identity),
            ObjectValueField.LINK_CHILDREN.extName to childrenValues.orEmpty().map { it.identity }
        )
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

    var intUpb: Int?
        get() = vertex[INT_TYPE_UPB_PROPERTY]
        set(value) {
            vertex[INT_TYPE_UPB_PROPERTY] = value
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

    var booleanValue: Boolean?
        get() = vertex[BOOL_TYPE_PROPERTY]
        set(value) {
            vertex[BOOL_TYPE_PROPERTY] = value
        }
    private val booleanValueStrict: Boolean
        get() = booleanValue ?: throw BooleanValueNodDefinedException(id)


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

    var decimalUpb: BigDecimal?
        get() = vertex[DECIMAL_TYPE_UPB_PROPERTY]
        set(value) {
            vertex[DECIMAL_TYPE_UPB_PROPERTY] = value
        }

    private val decimalUpbStrict: BigDecimal
        get() = decimalUpb ?: throw DecimalValueNotDefinedException(id)

    var precision: Int?
        get() = vertex[PRECISION_PROPERTY]
        set(v) = setOrRemove(PRECISION_PROPERTY, v)

    var leftInfinity: Boolean
        get() = vertex[RANGE_LEFT_INF_PROPERTY] ?: false
        set(value) {
            vertex[RANGE_LEFT_INF_PROPERTY] = value
        }

    var rightInfinity: Boolean
        get() = vertex[RANGE_RIGHT_INF_PROPERTY] ?: false
        set(value) {
            vertex[RANGE_RIGHT_INF_PROPERTY] = value
        }

    val objectProperty: ObjectPropertyVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_OBJECT_PROPERTY_EDGE).firstOrNull()
            ?.toObjectPropertyVertex()

    val aspectProperty: AspectPropertyVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_ASPECT_PROPERTY_EDGE).firstOrNull()
            ?.toAspectPropertyVertex()

    val parentValue: ObjectPropertyValueVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_OBJECT_VALUE_EDGE).firstOrNull()
            ?.toObjectPropertyValueVertex()

    private val childrenValues: List<ObjectPropertyValueVertex>?
        get() = vertex.getVertices(ODirection.IN, OBJECT_VALUE_OBJECT_VALUE_EDGE)
            ?.map { it.toObjectPropertyValueVertex() }

    val refValueObject: ObjectVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_OBJECT_EDGE).firstOrNull()?.toObjectVertex()

    val refValueObjectProperty: ObjectPropertyVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_REF_OBJECT_PROPERTY_EDGE).firstOrNull()?.toObjectPropertyVertex()

    val refValueObjectValue: ObjectPropertyValueVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_REF_OBJECT_VALUE_EDGE).firstOrNull()?.toObjectPropertyValueVertex()

    val refValueSubject: SubjectVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_SUBJECT_EDGE).firstOrNull()?.toSubjectVertex()

    val refValueDomainElement: ReferenceBookItemVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_DOMAIN_ELEMENT_EDGE).firstOrNull()?.toReferenceBookItemVertex()

    val refValueAspect: AspectVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_ASPECT_EDGE).firstOrNull()?.toAspectVertex()

    val refValueAspectProperty: AspectPropertyVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_REF_ASPECT_PROPERTY_EDGE).firstOrNull()?.toAspectPropertyVertex()

    val measure: OVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_MEASURE_EDGE).firstOrNull()

    val children: List<ObjectPropertyValueVertex>
        get() = vertex.getVertices(ODirection.IN, OBJECT_VALUE_OBJECT_VALUE_EDGE)
            .map { it.toObjectPropertyValueVertex() }.filterNot { it.deleted }

    val guid: String?
        get() = guid(OrientEdge.GUID_OF_OBJECT_VALUE)

    private fun calcRangeFlags(): Int {
        var result = 0

        if (leftInfinity) result += RangeFlagConstants.LEFT_INF.bitmask
        if (rightInfinity) result += RangeFlagConstants.RIGHT_INF.bitmask

        return result
    }

    fun toObjectPropertyValue(): ObjectPropertyValue {
        val currentProperty = objectProperty ?: throw ObjectValueWithoutPropertyException(this)
        val currentAspectProperty = aspectProperty

        val value: ObjectValue = when (typeTag) {
            ScalarTypeTag.OBJECT -> ObjectValue.Link(LinkValueVertex.Object(refValueObject ?: throw ObjectVertexNotDefinedException(id)))
            ScalarTypeTag.OBJECT_PROPERTY -> ObjectValue.Link(
                LinkValueVertex.ObjectProperty(
                    refValueObjectProperty ?: throw ObjectPropertyVertexNotDefinedException(id)
                )
            )
            ScalarTypeTag.OBJECT_VALUE -> ObjectValue.Link(LinkValueVertex.ObjectValue(refValueObjectValue ?: throw ObjectValueVertexNotDefinedException(id)))
            ScalarTypeTag.SUBJECT -> ObjectValue.Link(LinkValueVertex.Subject(refValueSubject ?: throw SubjectVertexNotDefinedException(id)))
            ScalarTypeTag.DOMAIN_ELEMENT -> {
                ObjectValue.Link(

                    LinkValueVertex.DomainElement(
                        refValueDomainElement ?: throw DomainElementVertexNotDefinedException(id)
                    )
                )
            }
            ScalarTypeTag.ASPECT -> ObjectValue.Link(LinkValueVertex.Aspect(refValueAspect ?: throw AspectVertexNotDefinedException(id)))
            ScalarTypeTag.ASPECT_PROPERTY -> ObjectValue.Link(
                LinkValueVertex.AspectProperty(
                    refValueAspectProperty ?: throw AspectPropertyVertexNotDefinedException(id)
                )
            )
            ScalarTypeTag.INTEGER -> ObjectValue.IntegerValue(intValueStrict, intUpb, precision)
            ScalarTypeTag.STRING -> ObjectValue.StringValue(strValueStrict)
            ScalarTypeTag.RANGE -> ObjectValue.RangeValue(rangeStrict)
            ScalarTypeTag.DECIMAL -> ObjectValue.DecimalValue.instance(decimalValueStrict, decimalUpb, calcRangeFlags())
            ScalarTypeTag.BOOLEAN -> ObjectValue.BooleanValue(booleanValueStrict)
            ScalarTypeTag.NULL -> ObjectValue.NullValue
            else ->
                throw IllegalStateException("type tag is not defined: $typeTag")
        }


        return ObjectPropertyValue(identity, value, currentProperty, currentAspectProperty, parentValue, measure, guid)
    }

    fun getOrCalculateMeasureSymbol(): String? {
        val currentMeasureSymbol = this.measure?.toMeasure()?.symbol
        return currentMeasureSymbol ?: run {
            val aspect = this.aspectProperty?.associatedAspect ?: this.objectProperty?.aspect
            aspect?.measure?.symbol
        }
    }

}

abstract class ObjectValueException(message: String) : Exception(message)
class ObjectValueWithoutPropertyException(vertex: ObjectPropertyValueVertex) :
    ObjectValueException("Object property vertex not linked for ${vertex.id} ")

class IntValueNotDefinedException(id: String) :
    ObjectValueException("int value is not defined for value $id")

class DecimalValueNotDefinedException(id: String) :
    ObjectValueException("decimal value is not defined for value $id")

class StringValueNotDefinedException(id: String) :
    ObjectValueException("string value is not defined for value $id")

class RangeNotDefinedException(id: String) :
    ObjectValueException("range is not defined for value $id")

class ObjectVertexNotDefinedException(id: String) :
    ObjectValueException("object vertex is not defined for value $id")

class AspectVertexNotDefinedException(id: String) :
    ObjectValueException("aspect vertex is not defined for value $id")

class AspectPropertyVertexNotDefinedException(id: String) :
    ObjectValueException("aspect property vertex is not defined for value $id")

class ObjectPropertyVertexNotDefinedException(id: String) :
    ObjectValueException("object property vertex is not defined for value $id")

class ObjectValueVertexNotDefinedException(id: String) :
    ObjectValueException("object value vertex is not defined for value $id")

class SubjectVertexNotDefinedException(id: String) :
    ObjectValueException("subject vertex is not defined for value $id")

class DomainElementVertexNotDefinedException(id: String) :
    ObjectValueException("domain element vertex is not defined for value $id")

class IncorrectTypeTagException(id: String, tag: Int) :
    ObjectValueException("incorrect type tag for object value $id, tag: $tag")

class BooleanValueNodDefinedException(id: String) :
    ObjectValueException("boolean value is not defined for value $id")

class ObjectValueIsLinkedException(ids: List<String>) :
    ObjectValueException("linked values: $ids")
