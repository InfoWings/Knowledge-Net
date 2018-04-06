package com.infowings.catalog

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class MasterCatalog

fun main(args: Array<String>) {
    // За кулисами загружаются beans, определенные в BeansInitializer
    SpringApplication.run(MasterCatalog::class.java, *args)
}

inline fun <reified T : Any> loggerFor(): Logger =
    LoggerFactory.getLogger(T::class.java.name) ?: throw Throwable("Cannot access to logger library")