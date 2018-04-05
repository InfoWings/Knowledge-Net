package com.infowings.catalog.common

enum class Direction(val dir: kotlin.Int) {
    ASC(1), DESC(-1)
}

enum class AspectSortField {
    NAME, SUBJECT
}

data class AspectOrderBy(val name: AspectSortField, val direction: Direction)