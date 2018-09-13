package com.infowings.catalog.objects.view.tree.format

import com.infowings.catalog.common.LinkValueData
import com.infowings.catalog.errors.showError
import com.infowings.catalog.objects.*
import com.infowings.catalog.utils.ApiException
import com.infowings.catalog.utils.BadRequestException
import com.infowings.catalog.wrappers.blueprint.Spinner
import kotlinx.coroutines.experimental.launch
import kotlinx.html.classes
import react.*
import react.dom.span

private const val ERROR_FETCH_BRIEF_VIEW_PLACEHOLDER = "Could not fetch entity by guid"

class ReferenceBaseTypeFormat : RComponent<ReferenceBaseTypeFormat.Props, ReferenceBaseTypeFormat.State>() {

    override fun State.init() {
        loading = true
    }

    override fun componentDidMount() {
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

    private suspend fun loadBriefView(value: LinkValueData) {
        val briefEntityInfo = when (value) {
            is LinkValueData.Object -> ObjectBriefInfo(getObjectBriefById(value.id))
            is LinkValueData.ObjectValue -> ValueBriefInfo(getValueBriefById(value.id))
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

    interface Props : RProps {
        var value: LinkValueData
    }

    interface State : RState {
        var loading: Boolean
        var briefInfo: EntityBriefInfo?
    }
}

fun RBuilder.referenceBaseTypeFormat(value: LinkValueData) = child(ReferenceBaseTypeFormat::class) {
    attrs {
        this.value = value
    }
}