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
        val userData = UserData(userConfig.username, userConfig.password, userConfig.role)
        return if (userData.username.isNotBlank() && userData.password.isNotBlank() && allowedRoles.contains(userData.role)) {
            logger.info("Valid user data for initialization: ${userData.safeToString()}")
            userData
        } else {
            logger.warn("Invalid user data for initialization: ${userData.safeToString()}")
            null
        }
    }

    private data class UserData(val username: String, val password: String, val role: String) {
        fun safeToString() = "UserData(username = \"$username\", role = \"$role\")"
    }
}

private val logger = loggerFor<UserInitApplicationListener>()
