package com.infowings.catalog.objects.treeview.inputs

import com.infowings.catalog.objects.Cardinality
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLSelectElement
import react.RBuilder
import react.dom.div
import react.dom.option
import react.dom.select


fun RBuilder.propertyCardinality(value: Cardinality?, onChange: (Cardinality) -> Unit) =
    div(classes = "pt-select object-property-input-cardinality") {
        select {
            attrs {
                attributes["value"] = value?.name ?: "" // TODO: Find more elegant solution
                onChangeFunction = { event ->
                    onChange(
                        when (event.target.unsafeCast<HTMLSelectElement>().value) {
                            "ZERO" -> Cardinality.ZERO
                            "ONE" -> Cardinality.ONE
                            "INFINITY" -> Cardinality.INFINITY
                            else -> error("Inconsistent state")
                        }
                    )
                }
            }
            if (value == null) {
                option {
                    attrs {
                        this.value = ""
                    }
                    +"Cardinality..."
                }
            }
            option {
                attrs {
                    this.value = Cardinality.ZERO.name
                }
                +"[0]"
            }
            option {
                attrs {
                    this.value = Cardinality.ONE.name
                }
                +"[0..1]"
            }
            option {
                attrs {
                    this.value = Cardinality.INFINITY.name
                }
                +"[0..∞]"
            }
        }
    }
