package com.infowings.catalog.common

import kotlinx.serialization.Serializable

enum class Direction(val dir: kotlin.Int) {
    ASC(1), DESC(-1)
}

enum class SortField {
    NAME, SUBJECT
}

@Serializable
data class SortOrder(val name: SortField, val direction: Direction) {
    companion object {
        fun listOf(orders: List<String>, directions: List<String>) = directions.zip(orders).map { (direction, order) ->
            SortOrder(SortField.valueOf(order), Direction.valueOf(direction))
        }
    }
}

data class ViewSlice(val offset: Int?, val limit: Int?) {
    fun next(total: Int = Int.MAX_VALUE) = when {
        offset != null && limit != null -> ViewSlice(minOf(offset + limit, total), limit)
        else -> this
    }

    fun prev() = when {
        offset != null && limit != null -> ViewSlice(maxOf(offset - limit, 0), limit)
        else -> this
    }
}

class CompareString(val value: String, val direction: Direction) : Comparable<CompareString> {
    override fun compareTo(other: CompareString): Int =
        direction.dir * value.toLowerCase().compareTo(other.value.toLowerCase())
}


