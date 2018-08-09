package com.infowings.catalog.auth.user

import com.infowings.catalog.common.User
import com.infowings.catalog.common.UserRole
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.USER_CLASS
import junit.framework.TestCase.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD, methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
class UserServiceTest {

    @Autowired
    private lateinit var db: OrientDatabase

    @Autowired
    private lateinit var userService: UserService

    private val user = User("test", "qwerty123", UserRole.USER)
    private val anotherUser = User("another", "123456", UserRole.POWERED_USER)

    private val userData = user.toUserData()

    @BeforeEach
    fun setUp() {
        // necessary to remove users created by initUsers method in OrientDatabaseInitializer class
        db.command("TRUNCATE CLASS $USER_CLASS UNSAFE") {}
    }

    @Test
    fun createUserTest() {
        assertEquals(user, userService.createUser(user))
    }

    @Test
    fun createUserFromUserDataTest() {
        assertEquals(user, userService.createUser(userData))
    }

    @Test
    fun createWithEmptyUsernameTest() {
        assertThrows<UsernameNullOrEmptyException> {
            userService.createUser(userData.copy(username = ""))
        }
    }

    @Test
    fun createWithNullUsernameTest() {
        assertThrows<UsernameNullOrEmptyException> {
            userService.createUser(userData.copy(username = null))
        }
    }

    @Test
    fun createWithEmptyPasswordTest() {
        assertThrows<PasswordNullOrEmptyException> {
            userService.createUser(userData.copy(password = ""))
        }
    }

    @Test
    fun createWithNullPasswordTest() {
        assertThrows<PasswordNullOrEmptyException> {
            userService.createUser(userData.copy(password = null))
        }
    }

    @Test
    fun createWithNullUserRoleTest() {
        assertThrows<UserRoleNullOrEmptyException> {
            userService.createUser(userData.copy(role = null))
        }
    }

    @Test
    fun createAlreadyExistUserTest() {
        userService.createUser(user)
        assertThrows<UserWithSuchUsernameAlreadyExist> {
            userService.createUser(user)
        }
    }

    @Test
    fun findByUsernameTest() {
        userService.createUser(user)
        assertEquals(user, userService.findByUsername(user.username))
    }

    @Test
    fun userNotFoundTest() {
        userService.createUser(user)
        assertThrows<UserNotFoundException> {
            userService.findByUsername("notExist")
        }
    }

    @Test
    fun getAllUsersTest() {
        userService.createUser(user)
        userService.createUser(anotherUser)
        assertEquals(setOf(user, anotherUser), userService.getAllUsers())
    }

    @Test
    fun changeRoleTest() {
        userService.createUser(user)
        val userWithNewRole = user.copy(role = UserRole.ADMIN)
        assertEquals(userWithNewRole, userService.changeRole(userWithNewRole))
    }

    @Test
    fun blockUserTest() {
        userService.createUser(user)
        val blockedUser = user.copy(blocked = true)
        assertEquals(blockedUser, userService.changeBlocked(blockedUser))
    }

    @Test
    fun changePasswordToEmptyTest() {
        userService.createUser(user)
        val userWithEmptyPassword = user.copy(password = "")
        assertThrows<PasswordNullOrEmptyException> {
            userService.changePassword(userWithEmptyPassword)
        }
    }

    @Test
    fun changePasswordTest() {
        userService.createUser(user)
        val userWithNewPassword = user.copy(password = "newPassword")
        assertEquals(userWithNewPassword, userService.changePassword(userWithNewPassword))
    }
}