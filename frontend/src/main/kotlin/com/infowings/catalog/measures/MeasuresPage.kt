package com.infowings.catalog.measures

import com.infowings.catalog.aspects.getSuggestedMeasurementUnits
import com.infowings.catalog.common.MeasureGroupDesc
import com.infowings.catalog.common.MeasureGroupMap
import com.infowings.catalog.components.searchbar.searchBar
import com.infowings.catalog.layout.header
import com.infowings.catalog.measures.treeview.measureTreeView
import com.infowings.catalog.wrappers.RouteSuppliedProps
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import react.RBuilder
import react.RComponent
import react.RState
import react.setState
import kotlin.browser.window

class MeasuresPage : RComponent<RouteSuppliedProps, MeasuresPage.State>() {

    private val allGroups = MeasureGroupMap.values
        .map {
            MeasureGroupData(
                it.name,
                it.description,
                it.measureList.map { UnitData(it.name, it.symbol, it.description, containsFilterText = true) })
        }

    override fun State.init() {
        filterText = ""
        groups = emptyList()
    }

    override fun componentDidMount() {
        setState {
            groups = allGroups
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
            groups = allGroups
        }
        filterText.length <= 2 -> setState {
            groups = filterGroupsBySymbol(filterText)
        }
        else -> {
            // if previous request not completed then cancel it
            job?.cancel()
            job = launch {
                val groups = filterGroupsByUnitOrMeasureName(filterText)
                setState {
                    this.groups = groups
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

    private suspend fun filterGroupsByUnitOrMeasureName(filterText: String): List<MeasureGroupData> {
        val filteredNames = getSuggestedMeasurementUnits(filterText, findInGroups = true)

        return filteredNames
            .map { name ->
                MeasureGroupData(
                    name,
                    MeasureGroupDesc[name],
                    allGroups.first { it.units.map { it.name }.contains(name) || it.name == name }.units
                        .map { it.copy(containsFilterText = (name == it.name)) }
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

        searchBar {
            attrs {
                filterText = state.filterText
                onFilterTextChange = ::handleFilterTextChange
            }
        }

        measureTreeView {
            attrs {
                groups = state.groups
            }
        }
    }

    interface State : RState {
        var filterText: String
        var groups: List<MeasureGroupData>
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