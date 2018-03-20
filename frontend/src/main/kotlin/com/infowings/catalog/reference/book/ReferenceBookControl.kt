package com.infowings.catalog.reference.book

import com.infowings.catalog.reference.book.treeview.referenceBookTreeView
import react.RBuilder
import react.RComponent
import react.RState

class ReferenceBookControl(props: ReferenceBookApiReceiverProps) :
    RComponent<ReferenceBookApiReceiverProps, RState>(props) {

    override fun RBuilder.render() {
        child(Popup::class) {
            attrs {
                closePopup = props.closePopup
            }

            referenceBookTreeView {
                attrs {
                    aspectId = props.aspectId
                    books = props.books
                    onReferenceBookCreate = props.onReferenceBookCreate
                    onReferenceBookUpdate = props.onReferenceBookUpdate
                }
            }
        }
    }
}