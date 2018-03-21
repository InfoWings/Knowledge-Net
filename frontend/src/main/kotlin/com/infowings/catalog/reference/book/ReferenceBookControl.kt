package com.infowings.catalog.reference.book

import com.infowings.catalog.reference.book.treeview.referenceBookTreeView
import react.RBuilder
import react.RComponent
import react.RState

class ReferenceBookControl(props: ReferenceBookApiReceiverProps) :
    RComponent<ReferenceBookApiReceiverProps, RState>(props) {

    override fun RBuilder.render() {
        referenceBookTreeView {
            attrs {
                rowDataList = props.aspectBookPairs
                onReferenceBookCreate = props.onReferenceBookCreate
                onReferenceBookUpdate = props.onReferenceBookUpdate
                createBookItem = props.createBookItem
            }
        }
    }
}