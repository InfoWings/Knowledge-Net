package com.infowings.catalog.measures

import com.infowings.catalog.aspects.getSuggestedMeasurementUnits
import com.infowings.catalog.common.MeasureGroupMap
import com.infowings.catalog.layout.Header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.h1
import react.setState
import kotlin.browser.window

class MeasuresPage : RComponent<RouteSuppliedProps, MeasuresPage.State>() {

    private val allData = MeasureGroupMap
        .flatMap { (measureGroupName, measureGroup) ->
            measureGroup.measureList.map {
                UnitsTableRowData(measureGroupName, it.name, it.symbol, containsFilterText = true)
            }
        }

    private val allDataMap = allData.groupBy { it.pivotBy }

    override fun State.init() {
        filterText = ""
        data = emptyList()
    }

    override fun componentDidMount() {
        setState {
            data = allData
        }
    }

    private var timer: Int = 0

    private fun handleFilterTextChange(filterText: String) {
        val before = state.filterText.length
        val after = filterText.length
        val skipUpdate = (before < 3 && after < 3)
        setState {
            this.filterText = filterText
        }
        if (!skipUpdate) {
            window.clearTimeout(timer)
            timer = window.setTimeout({ updateDataState(filterText) }, 200)
        }
    }

    private var job: Job? = null

    private fun updateDataState(filterText: String) {
        if (filterText.length < 3) {
            setState {
                this.data = allData
            }
        } else {
            // if previous request not completed then cancel it
            job?.cancel()
            job = launch {
                val data = getFilteredData(filterText)
                setState {
                    this.data = data
                }
            }
        }
    }

    private suspend fun getFilteredData(filterText: String): List<UnitsTableRowData> {
        val filteredNames = filterMeasureNames(filterText)

        // get list of measureGroupName of filtered measure names
        val measureGroupNames: List<String> = filteredNames
            .flatMap { name -> allData.filter { it.name == name || it.pivotBy == name }.map { it.pivotBy } }
            .distinct()

        // create map where key is measureGroupName and value is list of UnitsTableRowData of this group
        val dataByMeasureGroupNameMap: Map<String, List<UnitsTableRowData>> = measureGroupNames
            .mapNotNull { allDataMap[it] }
            .flatMap {
                it.map { UnitsTableRowData(it.pivotBy, it.name, it.symbol, filteredNames.contains(it.name)) }
            }.groupBy { it.pivotBy }

        return filteredNames
            .flatMap { name -> allData.filter { it.name == name || it.pivotBy == name }.map { name to it.pivotBy } }
            .distinct()
            .flatMap { (name, measureGroupName) ->
                dataByMeasureGroupNameMap.getValue(measureGroupName)
                    .map { UnitsTableRowData(name, it.name, it.symbol, it.containsFilterText) }
            }
    }

    private suspend fun filterMeasureNames(filterText: String): Array<String> {
        return getSuggestedMeasurementUnits(filterText, findInGroups = true)
    }

    override fun RBuilder.render() {
        child(Header::class) {
            attrs { location = props.location.pathname }
        }

        h1 { +"Measure Page" }

        child(SearchBar::class) {
            attrs {
                filterText = state.filterText
                onFilterTextChange = ::handleFilterTextChange
            }
        }

        child(UnitsTable::class) {
            attrs {
                data = state.data.toTypedArray()
            }
        }
    }

    interface State : RState {
        var filterText: String
        var data: List<UnitsTableRowData>
    }
}