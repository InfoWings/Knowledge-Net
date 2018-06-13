package com.infowings.catalog.common.history.refbook

import kotlinx.serialization.Serializable

class RefBookHistoryData {
    companion object {
        @Serializable
        data class Header(
            val id: String,
            val name: String,
            val description: String?,
            val aspectId: String,
            val aspectName: String
        )

        @Serializable
        data class Item(val id: String, val name: String, val description: String?)

        @Serializable
        data class BriefState(val header: Header, val item: Item?)
    }
}
