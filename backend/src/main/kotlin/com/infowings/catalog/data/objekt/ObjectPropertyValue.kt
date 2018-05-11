package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.ObjectPropertyValueData
import com.infowings.catalog.common.Range
import com.infowings.catalog.common.ScalarValue
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.id.ORID

/**
 * Object property value data representation for use in backend context.
 * It can use Orient data structures but it is detached from database - updated to it does not lead to
 * updates in database
 */
data class ObjectPropertyValue(
    val id: ORID?,
    val scalarValue: ScalarValue?,
    val range: Range?,
    val precision: Int?,
    val objectProperty: ObjectPropertyVertex,
    val rootCharacteristic: AspectVertex,
    val parentValue: ObjectPropertyValueVertex?
) {
    fun toObjectPropertyValueData(): ObjectPropertyValueData {
        return ObjectPropertyValueData(id?.toString(), scalarValue,
            range, precision, objectProperty.id, rootCharacteristic.id, parentValue?.id)
    }
}
