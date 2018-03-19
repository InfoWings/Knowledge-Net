package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class ReferenceBooksList(val books: List<ReferenceBook>)

@Serializable
data class ReferenceBookData(val id: String?, val name: String, val aspectId: String)