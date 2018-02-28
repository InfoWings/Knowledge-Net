package com.infowings.catalog.units

import com.infowings.catalog.common.MeasureGroupMap
import com.infowings.catalog.layout.Header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.h1
import react.setState
import kotlin.js.Json
import kotlin.js.json

class UnitsPage : RComponent<RouteSuppliedProps, UnitsPage.State>() {

    override fun State.init() {
        filterText = ""
    }

    private fun handleFilterTextChange(filterText: String) {
        setState {
            this.filterText = filterText
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
                data = getFilteredData(state.filterText)
                defaultExpandedRows = defineExpandedRows(state.filterText)
            }
        }
    }

    interface State : RState {
        var filterText: String
    }
}

private fun getFilteredData(filterText: String): Array<UnitsTableRowData> {
    val filterTextInLowerCase = filterText.toLowerCase()
    return MeasureGroupMap
        .filter {
            it.value.measureList.any { measure -> measure.name.toLowerCase().contains(filterTextInLowerCase) }
        }
        .flatMap {
            it.value.measureList.map { measure ->
                val containsFilterText =
                    filterTextInLowerCase.isEmpty() || measure.name.toLowerCase().contains(filterTextInLowerCase)
                UnitsTableRowData(it.key, measure.name, measure.symbol, containsFilterText)
            }
        }
        .toTypedArray()
}

private fun defineExpandedRows(filterText: String): Json {
    if (filterText.isEmpty()) return json()

    val rowsGroupCount = MeasureGroupMap
        .filter {
            it.value.measureList.any { measure -> measure.name.toLowerCase().contains(filterText.toLowerCase()) }
        }.count()

    val list = IntRange(0, rowsGroupCount)
        .map { json(it.toString() to true) }

    val result = json()

    for (json in list) {
        result.add(json)
    }
    return result
}