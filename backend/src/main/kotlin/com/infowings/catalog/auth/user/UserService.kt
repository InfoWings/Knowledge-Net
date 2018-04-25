package com.infowings.catalog.auth.user

import com.infowings.catalog.common.User
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction

class UserService(private val db: OrientDatabase, private val dao: UserDao) {

    fun createUser(user: User): User {
        logger.debug("Creating user: $user")

        return transaction(db) {
            if (dao.findByUsername(user.username) != null) throw UserWithSuchUsernameAlreadyExist(user.username)

            val userVertex = dao.createUserVertex().apply {
                username = user.username
                password = user.password
                role = user.role.name
                blocked = false
            }
            return@transaction dao.saveUserVertex(userVertex)
        }.toUser()
    }

    fun updateUser(user: User): User {
        logger.debug("Updating user: $user")

        return transaction(db) {
            val userVertex = findUserVertexByUsername(user.username)

            userVertex.apply {
                username = user.username
                password = user.password
                role = user.role.name
                blocked = user.blocked
            }
            return@transaction dao.saveUserVertex(userVertex)
        }.toUser()
    }

    fun findByUsername(username: String): User {
        logger.debug("Finding user by username: $username")
        return findUserVertexByUsername(username).toUser()
    }

    fun findUserVertexByUsername(username: String): UserVertex {
        logger.debug("Finding userVertex by username: $username")
        return dao.findByUsername(username) ?: throw UserNotFoundException(username)
    }

    fun getAllUsers(): Set<User> {
        logger.debug("Getting all users")
        return dao.getAllUserVertices().map { it.toUser() }.toSet()
    }
}

private val logger = loggerFor<UserService>()

sealed class UserException(message: String? = null) : Exception(message)

class UserNotFoundException(val username: String) : UserException("username: $username")
class UserWithSuchUsernameAlreadyExist(val username: String) : UserException("username: $username")