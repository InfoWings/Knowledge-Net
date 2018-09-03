@file:Suppress("MatchingDeclarationName") // TODO: Remove @Suppress when more classes will be in the file
package com.infowings.catalog.data.aspect

data class AspectPropertyDeleteResult(
    val aspectPropertyVertex: AspectPropertyVertex,
    val parentAspectVertex: AspectVertex,
    val childAspectVertex: AspectVertex
)