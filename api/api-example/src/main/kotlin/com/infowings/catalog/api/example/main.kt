package com.infowings.catalog.api.example

import com.infowings.catalog.api.KnowledgeNet

/**
 * Usage: provide server, port, user, password via corresponding system properties
 * -Dkn.server= -Dkn.server.port= -Dkn.server.user= -Dkn.server.password=
 */
fun main(args: Array<String>) {
    val server = System.getProperty("kn.server")
    val port = System.getProperty("kn.server.port").toInt()
    val user = System.getProperty("kn.server.user")
    val password = System.getProperty("kn.server.password")


    val api = KnowledgeNet(server, port, user, password)
    api.aspectApi.getAspects().aspects.forEach { println(it) }
}