package com.infowings.catalog.storage

import com.orientechnologies.orient.core.record.OVertex

fun OVertex.vertexClass(): String {
    return if (this.schemaType.isPresent) {
        this.schemaType.get().name
    } else "<UNKNOWN>"
}

fun OVertex.ofClass(orientClass: OrientClass): Boolean {
    return if (this.schemaType.isPresent) {
        this.schemaType.get().name == orientClass.extName
    } else false
}

fun OVertex.checkClass(orientClass: OrientClass) {
    if (!this.ofClass(orientClass)) throw IllegalStateException("vertex with id ${this.id} is of class ${this.schemaType}")
}

fun OVertex.checkClassAny(orientClasses: List<OrientClass>) {
    if (!orientClasses.any { this.ofClass(it) }) throw IllegalStateException("vertex with id ${this.id} is of class ${this.schemaType}")
}
