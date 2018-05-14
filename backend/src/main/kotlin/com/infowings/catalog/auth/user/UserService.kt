package com.infowings.catalog.auth.user

import com.infowings.catalog.common.User
import com.infowings.catalog.common.UserData
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction

class UserService(private val db: OrientDatabase, private val dao: UserDao) {

    private val validator = UserValidator()

    fun createUser(user: User): User {
        return createUser(user.toUserData())
    }

    fun createUser(userData: UserData): User {
        logger.debug("Creating user: $userData")

        return transaction(db) {

            val user = userData
                .checkUserDataConsistency()
                .toUser()

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

    fun changeRole(user: User): User {
        logger.debug("Change role for ${user.username} to ${user.role}")

        return transaction(db) {
            val userVertex = findUserVertexByUsername(user.username)
                .apply { role = user.role.name }

            return@transaction dao.saveUserVertex(userVertex)

        }.toUser()
    }

    fun changeBlocked(user: User): User {
        logger.debug("Change blocked for ${user.username} to ${user.blocked}")

        return transaction(db) {
            val userVertex = findUserVertexByUsername(user.username)
                .apply { blocked = user.blocked }

            return@transaction dao.saveUserVertex(userVertex)

        }.toUser()
    }

    fun changePassword(user: User): User {
        logger.debug("Change password for ${user.username}")

        return transaction(db) {
            user.checkPassword()

            val userVertex = findUserVertexByUsername(user.username)
                .apply { password = user.password }

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

    private fun UserData.checkUserDataConsistency() = this.also { validator.checkUserDataConsistency(it) }

    private fun User.checkPassword() = this.also { validator.checkPassword(it) }

}

fun User.toUserData(): UserData = UserData(username, password, role)

fun UserData.toUser(): User =
    User(
        username = username ?: throw UsernameNullOrEmptyException(),
        password = password ?: throw PasswordNullOrEmptyException(),
        role = role ?: throw UserRoleNullOrEmptyException()
    )

private val logger = loggerFor<UserService>()

sealed class UserException(message: String? = null) : Exception(message)

class UserNotFoundException(val username: String) : UserException("username: $username")
class UserWithSuchUsernameAlreadyExist(val username: String) : UserException("username: $username")
class UsernameNullOrEmptyException : UserException("Username should not be empty")
class PasswordNullOrEmptyException : UserException("Password should not be empty")
class UserRoleNullOrEmptyException : UserException("User role should not be empty")