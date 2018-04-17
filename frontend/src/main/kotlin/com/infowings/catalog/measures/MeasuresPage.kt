package com.infowings.catalog.measures

import com.infowings.catalog.aspects.getSuggestedMeasureData
import com.infowings.catalog.common.MeasureDesc
import com.infowings.catalog.common.MeasureGroupDesc
import com.infowings.catalog.common.MeasureGroupMap
import com.infowings.catalog.components.searchbar.searchBar
import com.infowings.catalog.layout.header
import com.infowings.catalog.measures.treeview.measureTreeView
import com.infowings.catalog.wrappers.RouteSuppliedProps
import kotlinext.js.require
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.div
import react.setState
import kotlin.browser.window

class MeasuresPage : RComponent<RouteSuppliedProps, MeasuresPage.State>() {

    companion object {
        init {
            require("styles/measures.scss")
        }
    }

    private val allGroups = MeasureGroupMap.values
        .map {
            MeasureGroupData(
                it.name,
                it.description,
                it.measureList.map { UnitData(it.name, it.symbol, it.description, containsFilterText = true) })
        }

    override fun State.init() {
        filterText = ""
        unitGroups = emptyList()
        measureGroups = emptyList()
    }

    override fun componentDidMount() {
        setState {
            unitGroups = allGroups
        }
    }

    private var timer: Int = 0

    private fun handleFilterTextChange(filterText: String) {
        setState {
            this.filterText = filterText
        }
        window.clearTimeout(timer)
        timer = window.setTimeout({ updateDataState(filterText) }, 200)
    }

    private var job: Job? = null

    private fun updateDataState(filterText: String) = when {
        filterText.isBlank() -> setState {
            unitGroups = allGroups
            measureGroups = emptyList()
        }
        filterText.length <= 2 -> setState {
            unitGroups = filterGroupsBySymbol(filterText)
            measureGroups = emptyList()
        }
        else -> {
            // if previous request not completed then cancel it
            job?.cancel()
            job = launch {
                val suggestedMeasureData = getSuggestedMeasureData(filterText, findInGroups = true)
                setState {
                    unitGroups = filterGroupsByMeasureName(suggestedMeasureData.measureNames)
                    measureGroups = filterGroupsByMeasureGroupName(suggestedMeasureData.measureGroupNames)
                }
            }
        }
    }

    private fun filterGroupsBySymbol(filterText: String): List<MeasureGroupData> {
        val unitNames = allGroups
            .flatMap { it.units.filter { it.symbol == filterText }.map { it.name } }

        return unitNames
            .map { name ->
                MeasureGroupData(
                    name,
                    MeasureGroupDesc[name],
                    allGroups.first { it.units.map { it.name }.contains(name) }.units
                        .map { it.copy(containsFilterText = (name == it.name)) }
                )
            }
    }

    private fun filterGroupsByMeasureName(measureNames: List<String>): List<MeasureGroupData> {
        return measureNames
            .map { name ->
                MeasureGroupData(
                    name,
                    MeasureDesc[name],
                    allGroups.first { it.units.map { it.name }.contains(name) }.units
                        .map { it.copy(containsFilterText = (name == it.name)) }
                )
            }
    }

    private fun filterGroupsByMeasureGroupName(measureGroupNames: List<String>): List<MeasureGroupData> {
        return measureGroupNames
            .map { groupName ->
                MeasureGroupData(
                    groupName,
                    MeasureGroupDesc[groupName],
                    allGroups.first { it.name == groupName }.units
                        .map { it.copy(containsFilterText = false) }
                )
            }
    }

    override fun RBuilder.render() {
        header {
            attrs {
                location = props.location.pathname
                history = props.history
            }
        }

        div(classes = "measures-page") {
            searchBar {
                attrs {
                    filterText = state.filterText
                    onFilterTextChange = ::handleFilterTextChange
                }
            }

            if (state.unitGroups.isNotEmpty() && state.measureGroups.isNotEmpty()) {
                div(classes = "columns-wrapper") {
                    div(classes = "left-column") {
                        measureTreeView {
                            attrs {
                                groups = state.unitGroups
                            }
                        }
                    }
                    div(classes = "right-column") {
                        measureTreeView {
                            attrs {
                                groups = state.measureGroups
                            }
                        }
                    }
                }
            } else {
                measureTreeView {
                    attrs {
                        groups = if (state.unitGroups.isNotEmpty()) state.unitGroups else state.measureGroups
                    }
                }
            }
        }
    }

    interface State : RState {
        var filterText: String
        var unitGroups: List<MeasureGroupData>
        var measureGroups: List<MeasureGroupData>
    }
}

data class MeasureGroupData(
    val name: String,
    val description: String?,
    val units: List<UnitData>
)

data class UnitData(
    val name: String,
    val symbol: String,
    val description: String?,
    val containsFilterText: Boolean
)