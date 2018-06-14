package com.infowings.catalog.objects.treeview.inputs

import com.infowings.catalog.common.PropertyCardinality
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLSelectElement
import react.RBuilder
import react.dom.div
import react.dom.option
import react.dom.select


fun RBuilder.propertyCardinality(value: PropertyCardinality?, onChange: (PropertyCardinality) -> Unit) =
    div(classes = "pt-select object-property-input-cardinality") {
        select {
            attrs {
                attributes["value"] = value?.name ?: "" // TODO: Find more elegant solution
                onChangeFunction = { event ->
                    onChange(
                        when (event.target.unsafeCast<HTMLSelectElement>().value) {
                            "ZERO" -> PropertyCardinality.ZERO
                            "ONE" -> PropertyCardinality.ONE
                            "INFINITY" -> PropertyCardinality.INFINITY
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
                    this.value = PropertyCardinality.ZERO.name
                }
                +PropertyCardinality.ZERO.label
            }
            option {
                attrs {
                    this.value = PropertyCardinality.ONE.name
                }
                +PropertyCardinality.ONE.label
            }
            option {
                attrs {
                    this.value = PropertyCardinality.INFINITY.name
                }
                +PropertyCardinality.INFINITY.label
            }
        }
    }
