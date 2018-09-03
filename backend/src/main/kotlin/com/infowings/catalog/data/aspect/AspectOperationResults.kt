package com.infowings.catalog.data.aspect

@Suppress("MatchingDeclarationName")
data class AspectPropertyDeleteResult(
    val aspectPropertyVertex: AspectPropertyVertex,
    val parentAspectVertex: AspectVertex,
    val childAspectVertex: AspectVertex
)