package com.infowings.catalog.auth.user

import com.infowings.catalog.common.UserRole
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "knet.users")
class UserProperties {
    var user = ArrayList<UserConfig>()
}

class UserConfig {
    lateinit var username: String
    lateinit var password: String
    lateinit var role: String
}

class UserDatabaseInitializer(private val database: OrientDatabase, private val userDao: UserDao) {

    private val allowedRoles = UserRole.values().map { it.toString() }.toSet()

    fun initUsers(userProperties: UserProperties) {
        transaction(database) {
            userProperties.user.mapNotNull(this::validateUserProperty).forEach { userData ->

                val userVertex = userDao.findByUsername(userData.username)?.let {
                    logger.info("User already exists: $userData")
                    it.apply {
                        password = userData.password
                        role = userData.role
                    }
                } ?: run {
                    logger.info("Initializing new user: $userData")
                    userDao.createUserVertex().apply {
                        username = userData.username
                        password = userData.password
                        role = userData.role
                        blocked = false
                    }
                }

                userDao.saveUserVertex(userVertex)
            }
        }
    }

    private fun validateUserProperty(userConfig: UserConfig): UserData? {
        return if (userConfig.username.isNotBlank() && userConfig.password.isNotBlank() && allowedRoles.contains(userConfig.role)) {
            val validUserData = UserData(userConfig.username, userConfig.password, userConfig.role)
            logger.info("Valid user data for initialization: $validUserData")
            validUserData
        } else null
    }

    private data class UserData(val username: String, val password: String, val role: String)
}

private val logger = loggerFor<UserInitApplicationListener>()
