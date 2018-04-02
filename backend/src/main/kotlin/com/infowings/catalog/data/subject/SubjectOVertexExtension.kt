package com.infowings.catalog.data.subject

import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.record.OVertex


fun OVertex.toSubjectVertex() = SubjectVertex(this)

class SubjectVertex(private val vertex: OVertex) : OVertex by vertex {

    var name: String
        get() = this["name"]
        set(value) {
            this["name"] = value
        }

    fun toSubject(): Subject =
        Subject(this.id, this.name)
}