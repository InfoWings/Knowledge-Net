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

    private val allMeasures = MeasureGroupMap
        .flatMap {
            it.value.measureList.map { measure ->
                UnitsTableRowData(it.key, measure.name, measure.symbol, true)
            }
        }
        .toTypedArray()

    override fun State.init() {
        filterText = ""
        data = emptyArray()
    }

    override fun componentDidMount() {
        setState {
            data = allMeasures
        }
    }

    private fun handleFilterTextChange(filterText: String) {
        setState {
            this.filterText = filterText
        }
        window.clearTimeout()
        window.setTimeout(updateDataState(filterText), 1000)
    }

    private fun updateDataState(filterText: String) {
        if (filterText.isEmpty()) {
            setState {
                this.data = allMeasures
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

    private suspend fun getFilteredData(filterText: String): Array<UnitsTableRowData> {
        val filteredMeasureNames = filterMeasureNames(filterText)
        return MeasureGroupMap
            .filter {
                it.value.measureList.any { measure -> filteredMeasureNames.contains(measure.name) }
            }
            .flatMap {
                it.value.measureList.map { measure ->
                    UnitsTableRowData(
                        it.key,
                        measure.name,
                        measure.symbol,
                        filteredMeasureNames.contains(measure.name)
                    )
                }
            }
            .toTypedArray()
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
                data = state.data
                defaultExpandedRows = defineExpandedRows(state.filterText)
            }
        }
    }

    interface State : RState {
        var filterText: String
        var data: Array<UnitsTableRowData>
    }
}

private fun defineExpandedRows(filterText: String): Json {
    if (filterText.isEmpty()) return json()

    val rowsGroupCount = MeasureGroupMap
        .filter {
            it.value.measureList.any { measure -> measure.name.toLowerCase().contains(filterText.toLowerCase()) }
        }.count()

    val list = IntRange(0, rowsGroupCount)
        .map { json(it.toString() to true) }

    val resultJson = json()

    for (json in list) {
        resultJson.add(json)
    }
    return resultJson
}