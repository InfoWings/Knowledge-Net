package com.infowings.catalog.data.aspect

data class AspectPropertyDeleteResult(
    val aspectPropertyVertex: AspectPropertyVertex,
    val parentAspectVertex: AspectVertex,
    val childAspectVertex: AspectVertex
)