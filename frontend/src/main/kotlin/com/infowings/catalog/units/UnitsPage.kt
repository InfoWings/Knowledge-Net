package com.infowings.catalog.units

import com.infowings.catalog.common.MeasureGroupMap
import com.infowings.catalog.layout.Header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import kotlinx.coroutines.experimental.launch
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.h1
import react.setState

class UnitsPage : RComponent<RouteSuppliedProps, UnitsPage.State>() {

    private val allData = MeasureGroupMap
        .flatMap { (measureGroupName, measureGroup) ->
            measureGroup.measureList.map {
                UnitsTableRowData(measureGroupName, it.name, it.symbol, containsFilterText = true)
            }
        }

    private val allDataMap = allData.groupBy { it.measureGroupName }

    override fun State.init() {
        data = emptyList()
    }

    override fun componentDidMount() {
        setState {
            data = allData
        }
    }

    private fun handleFilterTextChange(filterText: String) {
        if (filterText.isEmpty()) {
            setState {
                this.data = allData
            }
        } else {
            launch {
                val data = getFilteredData(filterText)
                setState {
                    this.data = data
                }
            }
        }
    }

    private suspend fun getFilteredData(filterText: String): List<UnitsTableRowData> {
        val filteredNames = filterMeasureNames(filterText)

        val measureGroupNames: Array<String> = filteredNames
            .flatMap { name -> allData.filter { it.name == name }.map { it.measureGroupName } }
            .distinct()
            .toTypedArray()

        return measureGroupNames
            .mapNotNull { allDataMap[it] }
            .flatMap {
                it.map { UnitsTableRowData(it.measureGroupName, it.name, it.symbol, filteredNames.contains(it.name)) }
            }
    }

    override fun RBuilder.render() {
        child(Header::class) {
            attrs { location = props.location.pathname }
        }

        h1 { +"Units Page" }

        child(SearchBar::class) {
            attrs {
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
        var data: List<UnitsTableRowData>
    }
}