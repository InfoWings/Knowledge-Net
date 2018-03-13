package com.infowings.catalog.subjects

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
import react.dom.h1
import react.dom.i
import react.dom.input

data class SubjectViewData(
    var name: String = "",
    var aspectNames: List<String>? = null,
    var aspectEditable: String = ""
)

class SubjectAspects(val data: SubjectViewData) : RComponent<RProps, RState>() {
    override fun RBuilder.render() {
        h1 { data.name }
    }
}

class SubjectsTable : RComponent<SubjectApiReceiverProps, SubjectsTable.State>() {

    override fun State.init(props: SubjectApiReceiverProps) {
        data = toSubjectViewData(props.data)
    }

    override fun RBuilder.render() {
        ReactTable {
            attrs {
                columns = arrayOf(
                    column("name", header("Subject name")),
                    RTableColumnDescriptor {
                        this.id = "aspects"
                        this.accessor = ::aspectsToString
                        this.Header = header("Aspect names")
                        this.Cell = rFunction("editAspectList") { props ->
                            div { +(props.row.aspects as String) }
                            input(type = InputType.text, classes = "rtable-input") {
                                attrs {
                                    value = props.row.aspectEditable
                                    placeholder = "start typing for add new aspect"
                                    contentEditable = true
                                    onBlurFunction = { onSubjectAspectAdd(props.index, it.asDynamic().target.value) }
                                }
                            }
                        }
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
        if (index < props.data.size) {
            val curSubjectData = props.data[index]
            props.onSubjectUpdate(SubjectData(curSubjectData.id, state.data[index].name, curSubjectData.aspects))
        } else {
            props.onSubjectsCreate(SubjectData(name = state.data.last().name))
        }
    }

    private fun onSubjectAspectAdd(index: Int, value: String) {

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
            data.map {
                SubjectViewData(it.name, it.aspects?.mapNotNull { it.name ?: "" }.toList())
            }.toTypedArray()
        }
}