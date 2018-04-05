package com.infowings.catalog

import com.infowings.catalog.auth.UserAcceptService
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.ReferenceBookService
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.search.SuggestionService
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.OrientHeartBeat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class MasterCatalog

class BeansInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(ctx: GenericApplicationContext) = beans {
        bean<UserAcceptService>()
        bean<MeasureService>()
        bean<ReferenceBookService>()
        bean<AspectDaoService>()
        bean<AspectService>()
        bean<SubjectService>()
        bean<SuggestionService>()
        bean<OrientDatabase>()
        bean<OrientHeartBeat>()
    }.initialize(ctx)
}

fun main(args: Array<String>) {
    SpringApplication.run(MasterCatalog::class.java, *args)
}

inline fun <reified T : Any> loggerFor(): Logger =
    LoggerFactory.getLogger(T::class.java.name) ?: throw Throwable("Cannot access to logger library")