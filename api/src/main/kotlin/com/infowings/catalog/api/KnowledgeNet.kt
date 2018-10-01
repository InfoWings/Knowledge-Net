package com.infowings.catalog.api

import com.infowings.catalog.api.impl.AspectApiWrapper

class KnowledgeNet(server: String, port: Int?, user: String, password: String) {
    private val serverContext = KNServerContext(server, port, user, password)

    val aspectApi: AspectApi by lazy { AspectApiWrapper(serverContext) }
}