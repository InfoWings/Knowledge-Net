package com.infowings.catalog.api

import com.infowings.catalog.api.impl.AspectApiWrapper
import com.infowings.catalog.api.impl.ObjectApiWrapper
import com.infowings.catalog.api.impl.SubjectApiWrapper

class KnowledgeNet(server: String, port: Int?, user: String, password: String) {
    private val serverContext = KNServerContext(server, port, user, password)

    val subjectApi: SubjectApi by lazy { SubjectApiWrapper(serverContext) }
    val aspectApi: AspectApi by lazy { AspectApiWrapper(serverContext) }
    val objectApi: ObjectApi by lazy { ObjectApiWrapper(serverContext) }
}