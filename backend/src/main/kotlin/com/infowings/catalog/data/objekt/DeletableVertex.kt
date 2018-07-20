package com.infowings.catalog.data.objekt

import com.infowings.catalog.storage.get
import com.orientechnologies.orient.core.record.OVertex

private val DELETED_PROPERTY = "deleted"

interface DeletableVertex : OVertex {
    var deleted: Boolean
        get() = getProperty(DELETED_PROPERTY) ?: false
        set(value) { setProperty(DELETED_PROPERTY, value) }

}