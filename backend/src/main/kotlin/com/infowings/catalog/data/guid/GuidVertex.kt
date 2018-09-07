package com.infowings.catalog.data.guid

import com.infowings.catalog.storage.OrientClass
import com.infowings.catalog.storage.checkClass
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.record.OVertex


fun OVertex.toGuidVertex(): GuidVertex {
    checkClass(OrientClass.GUID)
    return GuidVertex(this)
}

class GuidVertex(private val vertex: OVertex) : OVertex by vertex {
    var guid: String
        get() = vertex["guid"]
        set(value) {
            vertex["guid"] = value
        }
}
