package com.infowings.catalog.history

import com.infowings.catalog.common.history.EventKind
import com.infowings.catalog.common.history.HistoryData
import com.infowings.catalog.utils.ripIcon
import com.infowings.catalog.utils.userIcon
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.a
import react.dom.div
import react.dom.span
import kotlin.js.Date

class HistoryEventComponent : RComponent<HistoryEventComponent.Props, RState>() {

    private fun openFullVersion(e: Event) {
        console.log(props.historyData.fullData)
    }

    override fun RBuilder.render() {
        div("history-item") {
            div(classes = "history-item-field history-item--label-user") {
                userIcon("history-item-user-icon") { }
                span {
                    +props.historyData.user
                }
            }
            span(classes = "history-item-field history-item--label-event-${props.historyData.event.color}") {
                +props.historyData.event.name
            }

            span(classes = "history-item-field history-item--label-class") {
                +props.historyData.entityName
            }
            span(classes = "history-item-field history-item--label-guid") {
                a(classes = "no-underline") {
                    attrs.onClickFunction = ::openFullVersion
                    +props.historyData.info
                }
            }
            if (props.historyData.deleted) {
                ripIcon("history-item-field aspect-tree-view--rip-icon") {}
            }
            span(classes = "history-item-field history-item--label-timestamp") {
                +Date(props.historyData.timestamp).toDateString()
            }
            span(classes = "history-item-field history-item--label-version") {
                a(classes = "no-underline") {
                    attrs.onClickFunction = ::openFullVersion
                    +"ver. ${props.historyData.version}"
                }
            }
        }
    }

    interface Props : RProps {
        var historyData: HistoryData<*>
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