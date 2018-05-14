package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.Range
import com.infowings.catalog.common.ReferenceTypeGroup
import com.infowings.catalog.common.ScalarValue
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.aspect.toAspectVertex
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

class ObjectPropertyValueVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = OBJECT_PROPERTY_VALUE_CLASS

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            "range" to asStringOrEmpty(range),
            "precision" to asStringOrEmpty(precision)
        ),
        links = emptyMap()
    )

    /* Здесь храним тег простого типа, представимого как Int. Например. Int или какие-то варинты подмножеств Int
     * Значением этого поля определяется:
     *  - поле, в котором хранится значение
     *  - методы кодирования/декодирования
     */
    var intType: String?
        get() = vertex["intType"]
        set(value) { vertex["intType"] = value }
    private val intTypeStrict: String
       get() = intType ?: throw IntTypeNotDefinedException(id)

    /* Каждому типу соответствует свое поле. Храним в целочисленном формате.
     */
    var intValue: Int?
        get() = intType?.let {vertex[it]}
        set(value) { vertex[intTypeStrict] = value }
    private val intValueStrict: Int
        get() = intValue ?: throw IntValueNotDefinedException(id, intTypeStrict)


    var strType: String?
        get() = vertex["strType"]
        set(value) {
            vertex["strType"] = value
        }
    private val strTypeStrict: String
        get() = strType ?: throw StringTypeNotDefinedException(id)

    /* Каждому типу соответствует свое поле. Храним в строковом формате.
     */
    var strValue: String?
        get() = strType?.let {vertex[it]}
        set(value) {
            vertex[strTypeStrict] = value
        }

    val strValueStrict: String
        get() = strValue ?: throw StringValueNotDefinedException(id, strTypeStrict)



    var compoundType: String?
        get() = vertex["compoundType"]
        set(value) {
            vertex["compoundType"] = value
        }
    private val compoundTypeStrict: String
        get() = compoundType ?: throw CompoundTypeNotDefinedException(id)

    var compoundValue: String?
        get() = compoundType?.let {vertex["compound:$it"]}
        set(value) {
            vertex["compound:$compoundTypeStrict"] = value
        }
    private val compoundValueStrict: String
        get() = compoundValue ?: throw CompoundValueNotDefinedException(id, compoundTypeStrict)


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

    val rootCharacteristic: AspectVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_ASPECT_EDGE).firstOrNull()
            ?.toAspectVertex()

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
        val currentRootChar = rootCharacteristic ?: throw ObjectValueWithoutCharacteristicException(this)

        val intTypeCurrent = intType
        val strTypeCurrent = strType
        val compoundTypeCurrent = compoundType

        val simpleData: ScalarValue? = when {
            intTypeCurrent != null -> ScalarValue.IntegerValue(intValueStrict, intTypeCurrent)
            strTypeCurrent != null -> ScalarValue.StringValue(strValueStrict, strTypeCurrent)
            compoundTypeCurrent != null -> ScalarValue.CompoundValue(compoundValueStrict, compoundTypeCurrent)
            else -> null
        }

        val refValueVertex: ReferenceValueVertex? = refValueType ?. let {
            when (it) {
                ReferenceTypeGroup.SUBJECT.name ->
                    ReferenceValueVertex.SubjectValue(refValueSubject ?: throw SubjectVertexNotDefinedException(id))
                ReferenceTypeGroup.OBJECT.name ->
                    ReferenceValueVertex.ObjectValue(refValueObject ?: throw ObjectVertexNotDefinedException(id))
                ReferenceTypeGroup.DOMAIN_ELEMENT.name ->
                    ReferenceValueVertex.DomainElementValue(refValueDomainElement
                            ?: throw DomainElementVertexNotDefinedException(id))
                else -> throw IllegalStateException("unknown reference value vertex type: $refValueType")
            }
        }

        return ObjectPropertyValue(identity, simpleData,
            range, precision, currentProperty, currentRootChar, parentValue, refValueVertex, measure)
    }
}

abstract class ObjectValueException(message: String) : Exception(message)
class ObjectValueWithoutPropertyException(vertex: ObjectPropertyValueVertex) :
    ObjectValueException("Object property vertex not linked for ${vertex.id} ")
class ObjectValueWithoutCharacteristicException(vertex: ObjectPropertyValueVertex) :
    ObjectValueException("Characteristic vertex not linked for ${vertex.id} ")

class IntTypeNotDefinedException(id: String) : ObjectValueException("int type is not defined for value $id")
class IntValueNotDefinedException(id: String, typeName: String) :
    ObjectValueException("int value is not defined for value $id, type $typeName")

class StringTypeNotDefinedException(id: String) : ObjectValueException("string type is not defined for value $id")
class StringValueNotDefinedException(id: String, typeName: String) :
    ObjectValueException("string value is not defined for value $id, type $typeName")

class CompoundTypeNotDefinedException(id: String) : ObjectValueException("compound type is not defined for value $id")
class CompoundValueNotDefinedException(id: String, typeName: String) :
    ObjectValueException("compound value is not defined for value $id, type $typeName")

class ObjectVertexNotDefinedException(id: String) :
    ObjectValueException("object vertex is not defined for value $id")
class SubjectVertexNotDefinedException(id: String) :
    ObjectValueException("subject vertex is not defined for value $id")
class DomainElementVertexNotDefinedException(id: String) :
    ObjectValueException("domain element vertex is not defined for value $id")
