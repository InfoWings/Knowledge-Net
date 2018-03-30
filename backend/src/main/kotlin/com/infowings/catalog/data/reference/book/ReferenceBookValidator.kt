package com.infowings.catalog.data.reference.book

import com.infowings.catalog.common.ReferenceBookItemData

class ReferenceBookValidator {

    fun checkReferenceBookItemVersion(bookItemVertex: ReferenceBookItemVertex, bookItemData: ReferenceBookItemData) =
        bookItemVertex.checkRefBookItemVersion(bookItemData)

    private fun ReferenceBookItemVertex.checkRefBookItemVersion(bookItemData: ReferenceBookItemData) =
        this.also {
            if (version != bookItemData.version) {
                throw RefBookItemConcurrentModificationException(bookItemData.id, version, bookItemData.version)
            }

            //todo: check versions of all root children if it is need
            //or maybe we can update version of book when some of his child is updated
            //then it is sufficient to check only book version
        }

}