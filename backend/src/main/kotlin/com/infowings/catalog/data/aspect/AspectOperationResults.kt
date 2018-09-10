@file:Suppress("MatchingDeclarationName") // TODO: Remove @Suppress when more classes will be in the file
package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.AspectPropertyDeleteResponse
import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.common.objekt.Reference
import com.infowings.catalog.storage.id

data class AspectPropertyDeleteResult(
    val aspectPropertyVertex: AspectPropertyVertex,
    val parentAspectVertex: AspectVertex,
    val childAspectVertex: AspectVertex
) {

    fun toResponse() = AspectPropertyDeleteResponse(
        this.aspectPropertyVertex.id,
        PropertyCardinality.valueOf(this.aspectPropertyVertex.cardinality),
        this.aspectPropertyVertex.name,
        Reference(this.parentAspectVertex.id, this.parentAspectVertex.version),
        Reference(this.childAspectVertex.id, this.childAspectVertex.version)
    )

}