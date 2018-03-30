package com.infowings.catalog.data.reference.book

import com.infowings.catalog.common.ReferenceBookItem


class ReferenceBookValidator {

    fun checkReferenceBookItemVersion(bookItemVertex: ReferenceBookItemVertex, bookItem: ReferenceBookItem) =
        bookItemVertex.checkRefBookItemVersion(bookItem)

    private fun ReferenceBookItemVertex.checkRefBookItemVersion(bookItem: ReferenceBookItem) =
        this.also {
            if (version != bookItem.version) {
                throw RefBookItemConcurrentModificationException(bookItem.id, version, bookItem.version)
            }

            //todo: check versions of all root children if it is need
            //or maybe we can update version of book when some of his child is updated
            //then it is sufficient to check only book version
        }

}