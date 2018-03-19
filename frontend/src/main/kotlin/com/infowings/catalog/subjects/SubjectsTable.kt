package com.infowings.catalog.subjects

import com.infowings.catalog.aspects.AspectSuggestingInput
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.header
import com.infowings.catalog.wrappers.table.FilteringModel
import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import com.infowings.catalog.wrappers.table.ReactTable
import kotlinx.html.InputType
import kotlinx.html.js.onBlurFunction
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*

data class SubjectViewData(
    var name: String = "",
    var aspectNames: MutableList<String>? = null,
    var aspectEditable: AspectData = AspectData(name = "")
)

class SubjectsTable : RComponent<SubjectApiReceiverProps, SubjectsTable.State>() {

    override fun componentWillUpdate(nextProps: SubjectApiReceiverProps, nextState: State) {
        val size = nextProps.data.size + 1
        state.newAspects = Array(size, { AspectData(name = "") })
        state.subjectNames = Array(size, { "" })
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
                    }/*,
                    RTableColumnDescriptor {
                        this.accessor = "pending"
                        this.Header = addNewSubjectHeaderEnabled()
                        this.width = 55.0
                    }*/
                )
                data = if (props.loading) emptyArray() else subjectToRows()
                showPagination = false
                minRows = 2
                sortable = false
                showPageJump = false
                resizable = false
                collapseOnDataChange = false
                filterable = true
                onFilteredChange = { ewExpanded: dynamic, index: dynamic, event: dynamic ->
                    //filtering in server side
                    props.onFetchData((ewExpanded as Array<FilteringModel>).map { it.id to it.value }.toMap())
                }
                defaultFilterMethod =
                        { filter: dynamic, row: dynamic, column: dynamic -> true } //disable filtering in frontend
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
                        defaultValue = props.value?.toString() ?: ""
                        onBlurFunction = {
                            state.subjectNames[props.index] = it.asDynamic().target.value as String
                            onSaveChangedSubjectName(props.index)
                        }
                    }
                }

            }
        }

    private fun onSaveChangedSubjectName(index: Int) {
        setState {
            if (index < props.data.size) {
                if (state.subjectNames[index].isNotBlank()) {
                    val curSubjectData = props.data[index]
                    var aspects: List<AspectData> = curSubjectData.aspects
                    props.onSubjectUpdate(SubjectData(curSubjectData.id, state.subjectNames[index], aspects))
                }
            } else if (state.subjectNames.last().isNotBlank()) {
                props.onSubjectsCreate(SubjectData(name = state.subjectNames.last(), aspects = emptyList()))
                state.subjectNames += ""
            }
        }
    }

    private fun onSaveChangedAspects(index: Int) {
        setState {
            val newAspect: AspectData = state.newAspects[index]
            if (index < props.data.size) {
                    val curSubjectData = props.data[index]
                    var aspects: List<AspectData> = curSubjectData.aspects
                if (!newAspect.name.isNullOrBlank()) {
                    aspects += state.newAspects[index]
                }
                props.onSubjectUpdate(SubjectData(curSubjectData.id, curSubjectData.name, aspects))
            } else {
                props.onSubjectsCreate(SubjectData(name = state.subjectNames.last(), aspects = listOf(newAspect)))
                state.subjectNames += ""
            }
        }
    }

    private fun subjectToRows(): Array<SubjectViewData> {
        return toSubjectViewData(props.data) + arrayOf(SubjectViewData())
    }

    interface State : RState {
        var subjectNames: Array<String>
        var newAspects: Array<AspectData>
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

        child(AspectSuggestingInput::class) {
            attrs {
                associatedAspect = state.newAspects[props.index]
                onOptionSelected = {
                    onAspectChanged(props.index, it)
                }
                onAspectNameChanged = onAspectModified
            }
        }

        span(classes = "create-new-aspect-container") {
            i(classes = "fas fa-plus") {}
            attrs.onClickFunction = { onSaveChangedAspects(props.index) }
        }
    }

    private fun onAspectChanged(index: Int, value: AspectData) {
        setState {
            state.newAspects[index] = value
        }
    }

    private fun onAspectNameChanged(aspectData: AspectData, name: String) {

    }
}