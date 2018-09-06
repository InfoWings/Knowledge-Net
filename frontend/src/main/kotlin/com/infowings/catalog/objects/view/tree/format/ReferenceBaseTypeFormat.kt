package com.infowings.catalog.objects.view.tree.format

import com.infowings.catalog.common.LinkValueData
import com.infowings.catalog.wrappers.blueprint.Spinner
import kotlinx.html.classes
import react.*
import react.dom.span

private const val ERROR_FETCH_BRIEF_VIEW_PLACEHOLDER = "Could not fetch entity by guid"

class ReferenceBaseTypeFormat : RComponent<ReferenceBaseTypeFormat.Props, ReferenceBaseTypeFormat.State>() {

    override fun State.init() {
        loading = true
    }

    override fun componentDidMount() {
        loadBriefView(props.value)
    }

    private fun loadBriefView(value: LinkValueData) {
        when (value) {
            is LinkValueData.Object -> setState { briefView = "Object" }
            is LinkValueData.ObjectValue -> setState { briefView = "ObjectValue" }
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
            state.briefView?.let {
                attrs {
                    classes = setOf("object-property-value-line__link-value--loaded")
                }
                +it
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
        var briefView: String?
    }
}

fun RBuilder.referenceBaseTypeFormat(value: LinkValueData) = child(ReferenceBaseTypeFormat::class) {
    attrs {
        this.value = value
    }
}