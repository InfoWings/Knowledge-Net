package com.infowings.catalog.subjects

import com.infowings.catalog.aspects.AspectSuggestingInput
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.header
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import com.infowings.catalog.wrappers.table.ReactTable
import kotlinx.html.InputType
import kotlinx.html.contentEditable
import kotlinx.html.js.onBlurFunction
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.div
import react.dom.i
import react.dom.input
import react.dom.span

data class SubjectViewData(
    var name: String = "",
    var aspectNames: MutableList<String>? = null,
    var aspectEditable: AspectData = AspectData(name = "")
)

class SubjectsTable : RComponent<SubjectApiReceiverProps, SubjectsTable.State>() {

    override fun State.init(props: SubjectApiReceiverProps) {
        data = toSubjectViewData(props.data)
    }

    override fun RBuilder.render() {
        ReactTable {
            attrs {
                columns = arrayOf(
                    column("name", header("name")),
                    RTableColumnDescriptor {
                        this.id = "aspects"
                        this.accessor = ::aspectsToString
                        this.Header = header("aspects")
                        this.Cell = selectComponent(::onAspectChanged, ::onAspectNameChanged)
                        this.className = "aspect-cell"
                    },
                    RTableColumnDescriptor {
                        this.accessor = "pending"
                        this.Header = addNewSubjectHeaderEnabled()
                        this.width = 55.0
                    }
                )
                if (!props.loading && state.data == undefined) {
                    state.data = subjectToRows()
                }
                data = if (props.loading) emptyArray() else state.data
                showPagination = false
                minRows = 2
                sortable = false
                showPageJump = false
                resizable = false
                collapseOnDataChange = false
            }
        }
    }

    private fun aspectsToString(data: SubjectViewData): String {
        return data.aspectNames?.joinToString { it } ?: ""
    }

    private fun column(accessor: String, header: RClass<RTableRendererProps>) =
        RTableColumnDescriptor {
            this.accessor = accessor
            this.Header = header
            this.Cell = rFunction("SubjectField") { props ->
                input(type = InputType.text, classes = "rtable-input") {
                    attrs {
                        value = props.value?.toString() ?: ""
                        contentEditable = true
                        onChangeFunction = { onSubjectChanged(props.index, it.asDynamic().target.value) }
                        onBlurFunction = { onSaveChanged(props.index) }
                    }
                }
            }
        }

    private fun onSubjectChanged(index: Int, value: dynamic) {
        setState {
            data[index] = SubjectViewData(value, data[index].aspectNames)
        }
    }

    private fun onSaveChanged(index: Int) {
        setState {
            val newAspect: AspectData = state.data[index].aspectEditable
            val name: String = newAspect.name ?: ""
            if (index < props.data.size) {
                if (state.data[index].name.isNotBlank()) {
                    val curSubjectData = props.data[index]
                    var aspects: List<AspectData> = curSubjectData.aspects
                    if (name.isNotBlank()) {
                        state.data[index].aspectNames?.add(name)
                        aspects += newAspect
                    }
                    props.onSubjectUpdate(SubjectData(curSubjectData.id, state.data[index].name, aspects))
                }
            } else if (state.data.last().name.isNotBlank()) {
                val aspects = if (name.isNotBlank()) listOf<AspectData>(newAspect) else emptyList()
                props.onSubjectsCreate(SubjectData(name = state.data.last().name, aspects = aspects))
                state.data += SubjectViewData()
            }
        }
    }

    /**
     * Component that represents green "+" sign when there is no new aspect being edited.
     * Receives callback on click.
     */
    private fun addNewSubjectHeaderEnabled(): RClass<RTableRendererProps> =
        rFunction("CheckboxHeaderEnabled") {
            div(classes = "create-new-aspect-container") {
                i(classes = "fas fa-plus") {}
                attrs.onClickFunction = { startCreatingNewSubject() }
            }
        }

    private fun startCreatingNewSubject() {
        //TODO scroll down and focus to last row!
    }

    private fun subjectToRows(): Array<SubjectViewData> {
        if (state.editable == undefined) {
            state.editable = arrayOf(SubjectViewData())
        }
        return toSubjectViewData(props.data) + state.editable
    }


    interface State : RState {
        var data: Array<SubjectViewData>
        var editable: Array<SubjectViewData>
    }

    private fun toSubjectViewData(data: Array<SubjectData>): Array<SubjectViewData> =
        if (data == undefined) {
            emptyArray()
        } else {
            data.map { SubjectViewData(it.name, it.aspects?.mapNotNull { it.name ?: "" }.toMutableList()) }
                .toTypedArray()
        }

    private fun selectComponent(
        onAspectChanged: (index: Int, value: AspectData) -> Unit,
        onAspectModified: (AspectData, String) -> Unit
    ) = rFunction<RTableRendererProps>("AspectSelectField") { props ->
        div { +(props.row.aspects as String) }
/*
        input(type = InputType.text, classes = "rtable-input") {
            attrs {
                value = props.row.aspectEditable
                placeholder = "start typing for add new aspect"
                contentEditable = true
                onBlurFunction = { onSaveChanged(props.index, it.asDynamic().target.value) }
            }
        }
*/
        child(AspectSuggestingInput::class) {
            attrs {
                associatedAspect = (props.original as SubjectViewData).aspectEditable
                onOptionSelected = { onAspectChanged(props.index, it) }
                onAspectNameChanged = onAspectModified
            }
        }

        span(classes = "create-new-aspect-container") {
            i(classes = "fas fa-plus") {}
            attrs.onClickFunction = { onSaveChanged(props.index) }
        }
    }

    private fun onAspectChanged(index: Int, value: AspectData) {
        setState {
            state.data[index].aspectEditable = value
        }
    }

    private fun onAspectNameChanged(aspectData: AspectData, name: String) {

    }
}