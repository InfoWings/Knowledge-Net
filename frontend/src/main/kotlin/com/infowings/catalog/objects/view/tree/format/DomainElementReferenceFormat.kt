package com.infowings.catalog.objects.view.tree.format

import com.infowings.catalog.common.ReferenceBookItemPath
import com.infowings.catalog.reference.book.getReferenceBookItemPath
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import react.*
import react.dom.span

class DomainElementReferenceFormat : RComponent<DomainElementReferenceFormat.Props, DomainElementReferenceFormat.State>(),
    JobCoroutineScope by JobSimpleCoroutineScope() {

    override fun State.init() {
        isLoading = false
        path = ReferenceBookItemPath(emptyList())
    }

    override fun componentDidMount() {
        job = Job()
        launch {
            val referenceBookValuePath = getReferenceBookItemPath(props.id)
            setState {
                path = referenceBookValuePath
            }
        }
    }

    override fun componentWillUnmount() {
        job.cancel()
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
