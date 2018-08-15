package com.infowings.catalog.auth.user

import com.infowings.catalog.common.User
import com.infowings.catalog.common.UserRole
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.USER_CLASS
import io.kotlintest.shouldBe
import junit.framework.TestCase.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
class UserServiceTest {

    @Autowired
    private lateinit var db: OrientDatabase

    @Autowired
    private lateinit var userService: UserService

    private fun user() = User(randomName(), "qwerty123", UserRole.USER)
    private fun anotherUser() = User(randomName(), "123456", UserRole.POWERED_USER)

    private fun userData() = user().toUserData()

    @BeforeEach
    fun setUp() {
        // necessary to remove users created by initUsers method in OrientDatabaseInitializer class
        db.command("TRUNCATE CLASS $USER_CLASS UNSAFE") {}
    }

    @Test
    fun createUserTest() {
        val user = user()
        userService.createUser(user) shouldBe user
    }

    @Test
    fun createUserFromUserDataTest() {
        val user = user()
        userService.createUser(user.toUserData()) shouldBe user
    }

    @Test
    fun createWithEmptyUsernameTest() {
        assertThrows<UsernameNullOrEmptyException> {
            userService.createUser(userData().copy(username = ""))
        }
    }

    @Test
    fun createWithNullUsernameTest() {
        assertThrows<UsernameNullOrEmptyException> {
            userService.createUser(userData().copy(username = null))
        }
    }

    @Test
    fun createWithEmptyPasswordTest() {
        assertThrows<PasswordNullOrEmptyException> {
            userService.createUser(userData().copy(password = ""))
        }
    }

    @Test
    fun createWithNullPasswordTest() {
        assertThrows<PasswordNullOrEmptyException> {
            userService.createUser(userData().copy(password = null))
        }
    }

    @Test
    fun createWithNullUserRoleTest() {
        assertThrows<UserRoleNullOrEmptyException> {
            userService.createUser(userData().copy(role = null))
        }
    }

    @Test
    fun createAlreadyExistUserTest() {
        val user = user()
        userService.createUser(user)
        assertThrows<UserWithSuchUsernameAlreadyExist> {
            userService.createUser(user)
        }
    }

    @Test
    fun findByUsernameTest() {
        val user = user()
        userService.createUser(user)
        userService.findByUsername(user.username) shouldBe user
    }

    @Test
    fun userNotFoundTest() {
        userService.createUser(user())
        assertThrows<UserNotFoundException> {
            userService.findByUsername("notExist")
        }
    }

    @Test
    fun getAllUsersTest() {
        val user = user()
        userService.createUser(user)
        val anotherUser = anotherUser()
        userService.createUser(anotherUser)
        userService.getAllUsers() shouldBe setOf(user, anotherUser)
    }

    @Test
    fun changeRoleTest() {
        val user = user()
        userService.createUser(user)
        val userWithNewRole = user.copy(role = UserRole.ADMIN)
        userService.changeRole(userWithNewRole) shouldBe userWithNewRole
    }

    @Test
    fun blockUserTest() {
        val user = user()
        userService.createUser(user)
        val blockedUser = user.copy(blocked = true)
        assertEquals(blockedUser, userService.changeBlocked(blockedUser))
    }

    @Test
    fun changePasswordToEmptyTest() {
        userService.createUser(user())
        val userWithEmptyPassword = user().copy(password = "")
        assertThrows<PasswordNullOrEmptyException> {
            userService.changePassword(userWithEmptyPassword)
        }
    }

    @Test
    fun changePasswordTest() {
        val user = user()
        userService.createUser(user)
        val userWithNewPassword = user.copy(password = "newPassword")
        userService.changePassword(userWithNewPassword) shouldBe userWithNewPassword
    }
}