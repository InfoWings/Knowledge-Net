package com.infowings.catalog

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ImportResource("classpath:applicationContext.xml")
@ComponentScan(basePackages = ["com.infowings.catalog"])
@EnableAutoConfiguration
@EnableScheduling
class MasterCatalog

fun main(args: Array<String>) {
    SpringApplication.run(MasterCatalog::class, *args)
}

inline fun <reified T : Any> loggerFor(): Logger =
    LoggerFactory.getLogger(T::class.java.name) ?: throw Throwable("Cannot access to logger library")
