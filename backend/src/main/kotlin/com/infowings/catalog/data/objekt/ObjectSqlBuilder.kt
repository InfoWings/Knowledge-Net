package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.Range
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORID
import notDeletedSql
import java.math.BigDecimal


class ObjectSqlBuilder {

    val params = mutableMapOf<String, Any>()
    lateinit var sql: String

    fun fromObject(objectId: ORID, builder: ObjectTraverseBuilder.() -> Unit): ObjectTraverseBuilder {
        params["objectId"] = objectId
        val objTraverseBuilder = ObjectTraverseBuilder(":objectId", params)
        objTraverseBuilder.builder()
        sql = objTraverseBuilder.sql
        return objTraverseBuilder
    }

    fun fromObjProp(objectPropertyId: ORID, builder: ObjPropertyTraverseBuilder.() -> Unit): ObjPropertyTraverseBuilder {
        params["objectPropertyId"] = objectPropertyId
        val objPropTraverseBuilder = ObjPropertyTraverseBuilder(":objectPropertyId", params)
        objPropTraverseBuilder.builder()
        sql = objPropTraverseBuilder.sql
        return objPropTraverseBuilder
    }

    fun allObjects(builder: ObjectSelectBuilder.() -> Unit): ObjectSelectBuilder {
        val objSelectBuilder = ObjectSelectBuilder(OBJECT_CLASS, params)
        objSelectBuilder.builder()
        sql = objSelectBuilder.sql
        return objSelectBuilder
    }

    fun allObjProps(builder: ObjectPropertySqlBuilder.() -> Unit): ObjectPropertySqlBuilder {
        val objPropSqlBuilder = ObjectPropertySqlBuilder(OBJECT_PROPERTY_CLASS, params)
        objPropSqlBuilder.builder()
        sql = objPropSqlBuilder.sql
        return objPropSqlBuilder
    }
}

class ObjectSelectBuilder(sql: String, private val params: MutableMap<String, Any>, layer: Int = 0) : SqlConditionBuilder(sql, layer) {

    fun withName(name: String) {
        processConditions()
        params["name"] = name
        sql += "name = :name"
    }

    fun withSubjectLink(subjectId: ORID) {
        processConditions()
        params["linkedSubjectId"] = subjectId
        sql += ":linkedSubjectId in OUT(\"$OBJECT_SUBJECT_EDGE\")"
    }
}

class ObjectTraverseBuilder(var sql: String, private val params: MutableMap<String, Any>) {
    fun toObjProps(builder: ObjectPropertySqlBuilder.() -> Unit): ObjectPropertySqlBuilder {
        val objPropSqlBuilder = ObjectPropertySqlBuilder("(TRAVERSE IN(\"$OBJECT_OBJECT_PROPERTY_EDGE\") FROM $sql) WHERE \$depth>=1 ", params, 1)
        objPropSqlBuilder.builder()
        sql = objPropSqlBuilder.sql
        return objPropSqlBuilder
    }
}

class ObjPropertyTraverseBuilder(var sql: String, private val params: MutableMap<String, Any>) {
    fun toObjValues(builder: ObjectPropertyValueSqlBuilder.() -> Unit): ObjectPropertyValueSqlBuilder {
        val objPropValueSqlBuilder =
            ObjectPropertyValueSqlBuilder("(TRAVERSE IN(\"$OBJECT_VALUE_OBJECT_PROPERTY_EDGE\") FROM $sql) WHERE \$depth>=1 ", params, 1)
        objPropValueSqlBuilder.builder()
        sql = objPropValueSqlBuilder.sql
        return objPropValueSqlBuilder
    }
}

class ObjectPropertySqlBuilder(from: String, private val params: MutableMap<String, Any>, layer: Int = 0) : SqlConditionBuilder(from, layer) {

    fun withName(name: String?) {
        processConditions()
        if (name == null) {
            sql += "name is NULL"
        } else {
            params["name"] = name
            sql += "name = :name"
        }
    }

    fun withAspect(aspectId: ORID) {
        processConditions()
        params["aspectId"] = aspectId
        sql += ":aspectId IN OUT(\"$ASPECT_OBJECT_PROPERTY_EDGE\")"
    }
}

class ObjectPropertyValueSqlBuilder(from: String, private val params: MutableMap<String, Any>, layer: Int = 0) : SqlConditionBuilder(from, layer) {

    fun withName(name: String) {
        processConditions()
        params["name"] = name
        sql += "name = :name"
    }

    fun withIntValue(value: Int) {
        processConditions()
        params["intValue"] = value
        sql += "$INT_TYPE_PROPERTY = :intValue"
    }

    fun withDecimalValue(value: BigDecimal) {
        processConditions()
        params["decimalValue"] = value
        sql += "$DECIMAL_TYPE_PROPERTY = :decimalValue"
    }

    fun withStrValue(value: String) {
        processConditions()
        params["strValue"] = value
        sql += "$STR_TYPE_PROPERTY = :strValue"
    }

    fun withRange(value: Range) {
        processConditions()
        params["range"] = value
        sql += "$RANGE_TYPE_PROPERTY = :range"
    }

    fun withBooleanValue(value: Boolean) {
        processConditions()
        params["booleanValue"] = value
        sql += "$BOOL_TYPE_PROPERTY = :booleanValue"
    }

    fun withObjectLink(objectId: ORID) {
        processConditions()
        params["linkedObjectId"] = objectId
        sql += ":linkedObjectId in OUT(\"$OBJECT_VALUE_OBJECT_EDGE\")"
    }

    fun withObjectPropertyLink(objectPropertyId: ORID) {
        processConditions()
        params["linkedObjectPropertyId"] = objectPropertyId
        sql += ":linkedObjectPropertyId in OUT(\"$OBJECT_VALUE_REF_OBJECT_PROPERTY_EDGE\")"
    }

    fun withObjectValueLink(objectValueId: ORID) {
        processConditions()
        params["linkedObjectValueId"] = objectValueId
        sql += ":linkedObjectValueId in OUT(\"$OBJECT_VALUE_REF_OBJECT_VALUE_EDGE\")"
    }

    fun withAspectLink(aspectId: ORID) {
        processConditions()
        params["linkedAspectId"] = aspectId
        sql += ":linkedAspectId in OUT(\"$OBJECT_VALUE_ASPECT_EDGE\")"
    }

    fun withAspectPropertyLink(aspectPropertyId: ORID) {
        processConditions()
        params["linkedAspectPropertyId"] = aspectPropertyId
        sql += ":linkedAspectPropertyId in OUT(\"$OBJECT_VALUE_ASPECT_PROPERTY_EDGE\")"
    }

    fun withSubjectLink(subjectId: ORID) {
        processConditions()
        params["linkedSubjectId"] = subjectId
        sql += ":linkedSubjectId in OUT(\"$OBJECT_VALUE_SUBJECT_EDGE\")"
    }

    fun withDomainElementLink(refBookId: ORID) {
        processConditions()
        params["refBookId"] = refBookId
        sql += ":refBookId in OUT(\"$OBJECT_VALUE_DOMAIN_ELEMENT_EDGE\")"
    }

    fun withNullValue() {
        processConditions()
        sql += "typeTag is NULL"
    }
}

abstract class SqlConditionBuilder(from: String, layer: Int) {

    init {
        for (i in 0..(layer - 1)) {
            processConditions()
        }
    }

    var sql = "SELECT FROM $from"
    var conditions = 0

    fun withoutDeleted() {
        processConditions()
        sql += notDeletedSql
    }

    protected fun processConditions() {
        if (conditions == 0) {
            sql += " WHERE "
        }
        if (conditions > 0) {
            sql += " AND "
        }
        conditions++
    }
}

inline fun objectSqlBuilder(builder: ObjectSqlBuilder.() -> Unit): ObjectSqlBuilder {
    val sqlBuilder = ObjectSqlBuilder()
    sqlBuilder.builder()
    return sqlBuilder
}