package com.infowings.catalog.units

import com.infowings.catalog.common.MeasureGroupMap
import com.infowings.catalog.layout.Header
import com.infowings.catalog.wrappers.RouteSuppliedProps
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.h1
import react.setState

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
                data = MeasureGroupMap
                    .filter {
                        it.value.measureList.any { measure ->
                            measure.name.toLowerCase().contains(state.filterText.toLowerCase())
                        }
                    }
                    .flatMap {
                        it.value.measureList.map { measure ->
                            val filterText: String = state.filterText
                            val containsFilterText = filterText.isEmpty() ||
                                    (filterText.isNotEmpty() &&
                                            measure.name.toLowerCase().contains(filterText.toLowerCase()))
                            UnitsTableRowData(it.key, measure.name, measure.symbol, containsFilterText)
                        }
                    }
                    .toTypedArray()
            }
        }
    }

    interface State : RState {
        var filterText: String
    }
}