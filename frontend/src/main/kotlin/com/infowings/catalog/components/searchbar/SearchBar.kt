package com.infowings.catalog.components.searchbar

import kotlinext.js.require
import kotlinx.html.js.onChangeFunction
import react.*
import react.dom.div
import react.dom.input


class SearchBarProperties(
    var filterText: String,
    var onFilterTextChange: (filterText: String) -> Unit,
    var classes: String
) : RProps

class SearchBar(props: SearchBarProperties) : RComponent<SearchBarProperties, RState>(props) {

    companion object {
        init {
            require("styles/search-bar.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "search-bar") {
            input(classes = "search-bar--input") {
                this.attrs {
                    type = kotlinx.html.InputType.text
                    placeholder = "Search ..."
                    value = props.filterText
                    onChangeFunction = { props.onFilterTextChange(it.target.asDynamic().value as String) }
                }
            }
        }
    }
}

fun RBuilder.searchBar(block: RHandler<SearchBarProperties>) {
    child(SearchBar::class, block)
}