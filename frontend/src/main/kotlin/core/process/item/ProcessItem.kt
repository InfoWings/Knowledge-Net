package core.process.item

import com.infowings.common.SubjectInstanceDto
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.a
import react.dom.div

class ProcessItem : RComponent<ItemProps, ItemState>() {

    private fun clickItem() {
        setState {
            opened = !opened
        }
    }

    init {
        state.opened = false
    }

    override fun RBuilder.render() {
        a("#item-1", classes = "list-group-item") {
            attrs {
                // +"data-toggle:collapse"
                onClickFunction = { clickItem() }
            }
            +props.subject.state
        }
        if (state.opened && !props.subject.children.isEmpty()) {
            div("list-group") {
                props.subject.children.forEach {
                    processItem(it)
                }
            }
        }
    }
}

external interface ItemProps : RProps {
    var subject: SubjectInstanceDto
}

external interface ItemState : RState {
    var opened: Boolean
}

fun RBuilder.processItem(subjectInstanceDto: SubjectInstanceDto) = child(ProcessItem::class) {
    attrs {
        subject = subjectInstanceDto
    }
}