package com.infowings.catalog

import com.infowings.catalog.auth.user.UserDao
import com.infowings.catalog.auth.user.UserProperties
import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.HistoryDao
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.providers.*
import com.infowings.catalog.data.objekt.ObjectDaoService
import com.infowings.catalog.data.objekt.ObjectService
import com.infowings.catalog.data.reference.book.ReferenceBookDao
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.search.SuggestionService
import com.infowings.catalog.storage.DEFAULT_HEART_BEAT__TIMEOUT
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.OrientHeartBeat
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans
import org.springframework.core.env.get

/**
 * Выносим в отдельный файл и прописываем в application.properties
 * для того, чтобы beans были видны и в контексе приложения, и в контексте
 * тестов
 *
 * Цитата:
 * ----------------------------------------
 * The ApplicationContextInitializer approach seems to work for this basic use case,
 * but unlike previous example have chosen to use an context.initializer.classes entry
 * in the application.properties file, the big advantage compared to SpringApplication.addInitializers()
 * being that the beans are taken in account also for tests.
 * I have updated the related StackOverflow answer accordingly.
 * ------------------------------------------
 *
 * https://github.com/spring-projects/spring-boot/issues/8115#issuecomment-327829617
 * https://stackoverflow.com/questions/45935931/how-to-use-functional-bean-definition-kotlin-dsl-with-spring-boot-and-spring-w/46033685
 */
class BeansInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(ctx: GenericApplicationContext) = beans {
        bean { UserProperties() }
        bean { UserDao(db = ref()) }
        bean { UserService(db = ref(), dao = ref()) }
        bean { MeasureService(database = ref()) }
        bean { ReferenceBookDao(db = ref()) }
        bean { ReferenceBookService(db = ref(), dao = ref(), historyService = ref(), userService = ref()) }
        bean { AspectDaoService(db = ref(), measureService = ref()) }
        bean { SubjectDao(db = ref()) }
        bean { SubjectService(db = ref(), dao = ref(), history = ref(), userService = ref()) }
        bean {
            AspectService(
                db = ref(),
                aspectDaoService = ref(),
                referenceBookService = ref(),
                historyService = ref(),
                userService = ref()
            )
        }
        bean { SuggestionService(database = ref()) }
        bean { HistoryDao(db = ref()) }
        bean { HistoryService(db = ref(), historyDao = ref()) }
        bean { AspectConstructor(subjectService = ref(), referenceBookService = ref()) }
        bean { AspectDeltaConstructor(aspectService = ref()) }
        bean {
            AspectHistoryProvider(
                historyService = ref(),
                aspectConstructor = ref(),
                aspectDeltaConstructor = ref()
            )
        }
        bean {
            RefBookHistoryProvider(historyService = ref(), aspectDao = ref())
        }
        bean {
            ObjectHistoryProvider(historyService = ref(), aspectService = ref(), subjectService = ref())
        }
        bean { ObjectDaoService(db = ref()) }
        bean {
            ObjectService(
                db = ref(),
                dao = ref(),
                subjectService = ref(),
                userService = ref(),
                aspectDao = ref(),
                measureService = ref(),
                refBookService = ref(),
                historyService = ref()
            )
        }

        bean {
            env.systemProperties
            OrientDatabase(
                url = env["orient.url"],
                database = env["orient.database"],
                username = env["orient.user"],
                password = env["orient.password"],
                userProperties = ref()
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