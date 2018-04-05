package com.infowings.catalog

import com.infowings.catalog.auth.UserAcceptService
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.ReferenceBookService
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.search.SuggestionService
import com.infowings.catalog.storage.DEFAULT_HEART_BEAT__TIMEOUT
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.OrientHeartBeat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class MasterCatalog

class PropertyNotDefinedException(key: String) : Exception("property $key is not defined")

class BeansInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    private fun ConfigurableEnvironment.checkedProperty(key: String): String =
        getProperty(key) ?: throw PropertyNotDefinedException(key)

    override fun initialize(ctx: GenericApplicationContext) = beans {
        bean {
            UserAcceptService(database = ref())
        }

        bean {
            MeasureService(database = ref())
        }

        bean {
            ReferenceBookService(database = ref())
        }

        bean {
            AspectDaoService(db = ref(), measureService = ref())
        }

        bean {
            AspectService(db = ref(), aspectDaoService = ref())
        }

        bean {
            SubjectService(db = ref(), aspectService = ref())
        }

        bean {
            SuggestionService(database = ref())
        }

        bean {
            OrientDatabase(
                url = env.checkedProperty("orient.url"),
                database = env.checkedProperty("orient.database"),
                user = env.checkedProperty("orient.user"),
                password = env.checkedProperty("orient.password")
            )
        }

        bean {
            OrientHeartBeat(
                database = ref(),
                seconds = env.getProperty("orient.heartbeat.timeout")?.toInt() ?: DEFAULT_HEART_BEAT__TIMEOUT
            )
        }
    }.initialize(ctx)
}

fun main(args: Array<String>) {
    SpringApplication.run(MasterCatalog::class.java, *args)
}

inline fun <reified T : Any> loggerFor(): Logger =
    LoggerFactory.getLogger(T::class.java.name) ?: throw Throwable("Cannot access to logger library")