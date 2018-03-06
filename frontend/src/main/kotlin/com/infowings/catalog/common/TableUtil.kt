package com.infowings.catalog.common

import com.infowings.catalog.wrappers.table.RTableColumnDescriptor
import com.infowings.catalog.wrappers.table.RTableRendererProps
import react.RClass
import react.dom.span
import react.rFunction

fun header(columnName: String) = rFunction<RTableRendererProps>("UnitsTableHeader") {
    span {
        +columnName
    }
}

fun column(accessor: String, header: RClass<RTableRendererProps>) =
    RTableColumnDescriptor {
        this.accessor = accessor
        this.Header = header
    }