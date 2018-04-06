package com.infowings.catalog.history

import com.infowings.catalog.common.AspectDataView
import com.infowings.catalog.common.EventKind
import com.infowings.catalog.common.HistoryData
import com.infowings.catalog.utils.ripIcon
import com.infowings.catalog.utils.userIcon
import com.infowings.catalog.wrappers.blueprint.Collapse
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.a
import react.dom.div
import react.dom.span
import kotlin.js.Date

class HistoryEventComponent : RComponent<HistoryEventComponent.Props, HistoryEventComponent.State>() {

    override fun State.init(props: HistoryEventComponent.Props) {
        showFullVersion = false
    }

    override fun RBuilder.render() {
        div("history-item") {
            userIcon("history-item--icon") { }
            span("history-item--field") {
                +props.historyData.user
            }
            span(classes = "history-item--field history-item--field__${props.historyData.event.color}") {
                +props.historyData.event.name
            }

            span(classes = "history-item--field") {
                +props.historyData.entityName
            }
            span(classes = "history-item--field history-item--field__cursive") {
                +(props.historyData.info ?: "")
            }
            if (props.historyData.deleted) {
                ripIcon("history-item--field aspect-tree-view--rip-icon") {}
            }
            span(classes = "history-item--field") {
                +Date(props.historyData.timestamp).toDateString()
            }
            span(classes = "history-item--field history-item--field__pointer") {
                a {
                    attrs.onClickFunction = {
                        setState {
                            showFullVersion = !showFullVersion
                        }
                    }
                    +"ver. ${props.historyData.version}"
                }
            }
        }
        when (props.historyData.fullData) {
            is AspectDataView ->
                Collapse {
                    attrs {
                        className = "history-aspect-view--wrapper"
                        isOpen = state.showFullVersion
                    }
                    aspectFullContainer {
                        attrs {
                            view = props.historyData.fullData as AspectDataView
                            onExit = { setState { showFullVersion = false } }
                        }
                    }
                }

            else -> Unit
        }

        aspectDiffContainer {
            attrs {
                this.changes = props.historyData.changes
            }
        }
    }

    interface Props : RProps {
        var historyData: HistoryData<*>
    }

    interface State : RState {
        var showFullVersion: Boolean
    }
}

fun RBuilder.historyEventComponent(handler: RHandler<HistoryEventComponent.Props>) =
    child(HistoryEventComponent::class, handler)

val EventKind.color: String
    get() = when (this) {
        EventKind.CREATE -> "green"
        EventKind.UPDATE -> "yellow"
        EventKind.DELETE -> "red"
        EventKind.SOFT_DELETE -> "red"
    }