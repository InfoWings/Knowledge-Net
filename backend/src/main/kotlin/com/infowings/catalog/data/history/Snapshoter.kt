package com.infowings.catalog.data.history

import com.orientechnologies.orient.core.id.ORID

interface Snapshoter<in T> {
    fun dataExtractors(): List<Pair<String, (T) -> String>>
    fun linksExtractors(): List<Pair<String, (T) -> List<ORID>>>

    private fun <T, S> toPlainPayload(entity: T, extractors: List<Pair<String, (T) -> S>>): Map<String, S> =
        extractors.map { it.first to it.second(entity) }.toMap()

    fun snapshot(entity: T): Snapshot = Snapshot(
        toPlainPayload(entity, dataExtractors()),
        toPlainPayload(entity, linksExtractors())
    )

    fun emptySnapshot(): Snapshot = Snapshot(
        dataExtractors().map { it.first to "" }.toMap(),
        dataExtractors().map { it.first to emptyList<ORID>() }.toMap()
    )
}