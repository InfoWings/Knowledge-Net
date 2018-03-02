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
import kotlin.browser.window
import kotlin.js.Json
import kotlin.js.json

class UnitsPage : RComponent<RouteSuppliedProps, UnitsPage.State>() {

    private val allData = MeasureGroupMap
        .flatMap { (measureGroupName, measureGroup) ->
            measureGroup.measureList.map {
                UnitsTableRowData(measureGroupName, it.name, it.symbol, containsFilterText = true)
            }
        }

    private val allDataMap = allData.groupBy { it.measureGroupName }.toMap()

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
        setState {
            this.filterText = filterText
        }
        window.clearTimeout(timer)
        timer = window.setTimeout({ updateDataState(filterText) }, 500)
    }

    private fun updateDataState(filterText: String) {
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
            .map { allDataMap.getValue(it) }
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
                filterText = state.filterText
                onFilterTextChange = ::handleFilterTextChange
            }
        }

        child(UnitsTable::class) {
            attrs {
                data = state.data.toTypedArray()
                defaultExpandedRows = defineExpandedRows(state.data)
            }
        }
    }

    interface State : RState {
        var filterText: String
        var data: List<UnitsTableRowData>
    }
}

private fun defineExpandedRows(data: List<UnitsTableRowData>): Json {
    val rowsGroupCount = data.map { it.measureGroupName }.distinct().count()

    if (rowsGroupCount == MeasureGroupMap.count()) return json()

    val jsonList = (0 until rowsGroupCount).map { json(it.toString() to true) }

    val resultJson = json()
    for (json in jsonList) {
        resultJson.add(json)
    }
    return resultJson
}