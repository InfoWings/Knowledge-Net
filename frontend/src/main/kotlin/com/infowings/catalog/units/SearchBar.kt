package com.infowings.catalog.units

import kotlinx.html.js.onChangeFunction
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.form
import react.dom.input


class SearchBarProperties(var filterText: String, var onFilterTextChange: (filterText: String) -> Unit) : RProps

class SearchBar(props: SearchBarProperties) : RComponent<SearchBarProperties, RState>(props) {

    override fun RBuilder.render() {
        form {
            input {
                this.attrs {
                    type = kotlinx.html.InputType.text
                    placeholder = "Search unit..."
                    value = props.filterText
                    onChangeFunction = { event ->
                        props.onFilterTextChange(event.target.asDynamic().value)
                    }
                }
            }
        }
    }
}
