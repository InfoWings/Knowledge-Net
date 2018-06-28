package com.infowings.catalog.objects.view.tree.format

import com.infowings.catalog.common.ReferenceBookItemPath
import com.infowings.catalog.reference.book.getReferenceBookItemPath
import kotlinx.coroutines.experimental.launch
import react.*
import react.dom.span

class DomainElementReferenceFormat : RComponent<DomainElementReferenceFormat.Props, DomainElementReferenceFormat.State>() {

    override fun State.init() {
        isLoading = false
        path = ReferenceBookItemPath(emptyList())
    }

    override fun componentDidMount() {
        launch {
            val referenceBookValuePath = getReferenceBookItemPath(props.id)
            setState {
                path = referenceBookValuePath
            }
        }
    }

    override fun RBuilder.render() {
        span(classes = "object-property-value-line__value") {
            if (state.isLoading) {
                loadingStub {}
            } else {
                +state.path.path.joinToString(" → ") { it.value }
            }
        }
    }

    interface State : RState {
        var isLoading: Boolean
        var path: ReferenceBookItemPath
    }

    interface Props : RProps {
        var id: String
    }
}

fun RBuilder.domainElementReferenceFormat(id: String) = child(DomainElementReferenceFormat::class) {
    attrs {
        this.id = id
    }
}
