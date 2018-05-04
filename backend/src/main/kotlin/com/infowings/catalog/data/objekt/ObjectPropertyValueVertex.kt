package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
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
        set(value) {
            vertex["intType"] = value
        }
    val intTypeStrict: String
       get() = intType ?: throw IntTypeNotDefinedException(id)


    /* Каждому типу соответствует свое поле. Храним в строковом формате.
     */
    var intValue: Int?
        get() = intType?.let {vertex[it]}
        set(value) {
            vertex[intTypeStrict] = value
        }
    val intValueStrict: Int
        get() = intValue ?: throw IntValueNotDefinedException(id, intTypeStrict)


    var strType: String?
        get() = vertex["strType"]
        set(value) {
            vertex["strType"] = value
        }
    val strTypeStrict: String
        get() = strType ?: throw StringTypeNotDefinedException(id)

    /* Каждому типу соответствует свое поле. Храним в целочисленном формате.
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
    val compoundTypeStrict: String
        get() = compoundType ?: throw CompoundTypeNotDefinedException(id)

    var compoundValue: String?
        get() = compoundType?.let {vertex[it]}
        set(value) {
            vertex[compoundTypeStrict] = value
        }
    val compoundValueStrict: String
        get() = compoundValue ?: throw CompoundValueNotDefinedException(id, compoundTypeStrict)


    var range: Range?
        get() {
            val s: String = vertex["range"]
            val parts = s.split(":").map {it.toInt()}
            return Range(parts[0], parts[1])
        }
        set(value) {
            if (value != null) {
                vertex["range"] = "${value.left}:${value.right}"
            } else {
                vertex.removeProperty("range")
            }
        }

    var precision: Int?
        get() = vertex["precision"]
        set(v) {
            if (v != null) {
                vertex["precision"] = v
            } else {
                vertex.removeProperty("precision")
            }
        }

    val objectProperty: ObjectPropertyVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_OBJECT_PROPERTY_EDGE).firstOrNull()?.toObjectPropertyVertex()

    val characteristics: List<CharacteristicVertex>
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_CHARACTERISTIC_EDGE).map {
            it.toCharacteristicVertex()
        }

    fun toObjectPropertyValue(): ObjectPropertyValue {
        val currentProperty = objectProperty ?: throw ObjectValueWithoutPropertyException(this)

        val intTypeCurrent = intType
        val strTypeCurrent = strType
        val compoundTypeCurrent = compoundType

        val simpleData: ScalarValue? = when {
            intTypeCurrent != null -> ScalarValue.IntegerValue(intValueStrict, intTypeCurrent)
            strTypeCurrent != null -> ScalarValue.StringValue(strValueStrict, strTypeCurrent)
            compoundTypeCurrent != null -> ScalarValue.CompoundValue(compoundValueStrict, compoundTypeCurrent)
            else -> null
        }

        return ObjectPropertyValue(identity, simpleData,
            range, precision, currentProperty, characteristics)
    }
}

abstract class ObjectValueException(message: String) : Exception(message)
class ObjectValueWithoutPropertyException(vertex: ObjectPropertyValueVertex) :
    ObjectValueException("Object property vertex not linked for ${vertex.id} ")

class IntTypeNotDefinedException(id: String) : ObjectValueException("int type is not defined for value $id")
class IntValueNotDefinedException(id: String, typeName: String) :
    ObjectValueException("int value is not defined for value $id, type $typeName")

class StringTypeNotDefinedException(id: String) : ObjectValueException("string type is not defined for value $id")
class StringValueNotDefinedException(id: String, typeName: String) :
    ObjectValueException("string value is not defined for value $id, type $typeName")

class CompoundTypeNotDefinedException(id: String) : ObjectValueException("compound type is not defined for value $id")
class CompoundValueNotDefinedException(id: String, typeName: String) :
    ObjectValueException("compound value is not defined for value $id, type $typeName")
