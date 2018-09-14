package com.infowings.catalog.auth.user

import com.infowings.catalog.storage.OrientDatabase
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent

class UserInitApplicationListener : ApplicationListener<ContextRefreshedEvent> {

    override fun onApplicationEvent(event: ContextRefreshedEvent?) {
        event?.let {
            val database = it.applicationContext.getBean(OrientDatabase::class.java)
            val userDao = it.applicationContext.getBean(UserDao::class.java)
            val userProperties = it.applicationContext.getBean(UserProperties::class.java)

            UserDatabaseInitializer(database, userDao).initUsers(userProperties)
        }
    }
}

