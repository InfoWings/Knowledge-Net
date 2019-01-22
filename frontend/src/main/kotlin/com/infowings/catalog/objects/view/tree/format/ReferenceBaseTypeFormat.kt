package com.infowings.catalog.objects.view.tree.format

import com.infowings.catalog.common.LinkValueData
import com.infowings.catalog.errors.showError
import com.infowings.catalog.objects.*
import com.infowings.catalog.utils.ApiException
import com.infowings.catalog.utils.BadRequestException
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import com.infowings.catalog.wrappers.History
import com.infowings.catalog.wrappers.RouteSuppliedProps
import com.infowings.catalog.wrappers.blueprint.Spinner
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.html.classes
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.span
import react.setState

private const val ERROR_FETCH_BRIEF_VIEW_PLACEHOLDER = "Could not fetch entity by guid"

class ReferenceBaseTypeFormat : RComponent<ReferenceBaseTypeFormat.Props, ReferenceBaseTypeFormat.State>(), JobCoroutineScope by JobSimpleCoroutineScope() {

    override fun State.init() {
        loading = true
    }

    override fun componentDidMount() {
        job = Job()
        launch {
            try {
                loadBriefView(props.value)
            } catch (apiException: ApiException) {
                showError(apiException)
                setState {
                    briefInfo = null
                    loading = false
                }
            }
        }
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    private suspend fun loadBriefView(value: LinkValueData) {
        val briefEntityInfo = when (value) {
            is LinkValueData.Object -> ObjectBriefInfo(value.getObjectBriefById(), props.history, false)
            is LinkValueData.ObjectValue -> {
                ValueBriefInfo(getValueBriefById(value.id), props.history, false)
            }
            else -> {
                showError(BadRequestException("Link value $value is not yet supported", null))
                null
            }
        }
        setState {
            briefInfo = briefEntityInfo
            loading = false
        }
    }

    override fun RBuilder.render() {
        when (state.loading) {
            true -> renderSpinner()
            false -> renderSpan()
        }
    }

    private fun RBuilder.renderSpinner() {
        Spinner {
            attrs {
                small = true
            }
        }
    }

    private fun RBuilder.renderSpan() {
        span {
            state.briefInfo?.let {
                attrs {
                    classes = setOf("object-property-value-line__link-value--loaded entity-brief-info")
                }
                it.render(this)
            } ?: run {
                attrs {
                    classes = setOf("object-property-value-line__link-value--error")
                }
                +ERROR_FETCH_BRIEF_VIEW_PLACEHOLDER
            }
        }
    }

    interface Props : RouteSuppliedProps {
        var value: LinkValueData
    }

    interface State : RState {
        var loading: Boolean
        var briefInfo: EntityBriefInfo?
    }
}

fun RBuilder.referenceBaseTypeFormat(value: LinkValueData, history: History) = child(ReferenceBaseTypeFormat::class) {
    attrs {
        this.value = value
        this.history = history
    }
}