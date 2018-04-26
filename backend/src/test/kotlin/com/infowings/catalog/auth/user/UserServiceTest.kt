package com.infowings.catalog.auth.user

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.User
import com.infowings.catalog.common.UserData
import com.infowings.catalog.common.UserRole
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.USER_CLASS
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserServiceTest {

    @Autowired
    private lateinit var db: OrientDatabase

    @Autowired
    private lateinit var userService: UserService

    private val user = User("test", "qwerty123", UserRole.USER)
    private val anotherUser = User("another", "123456", UserRole.POWERED_USER)

    private val userData = user.toUserData()

    @Before
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

    @Test(expected = UsernameNullOrEmptyException::class)
    fun createWithEmptyUsernameTest() {
        userService.createUser(userData.copy(username = ""))
    }

    @Test(expected = UsernameNullOrEmptyException::class)
    fun createWithNullUsernameTest() {
        userService.createUser(userData.copy(username = null))
    }

    @Test(expected = PasswordNullOrEmptyException::class)
    fun createWithEmptyPasswordTest() {
        userService.createUser(userData.copy(password = ""))
    }

    @Test(expected = PasswordNullOrEmptyException::class)
    fun createWithNullPasswordTest() {
        userService.createUser(userData.copy(password = null))
    }

    @Test(expected = UserRoleNullOrEmptyException::class)
    fun createWithNullUserRoleTest() {
        userService.createUser(userData.copy(role = null))
    }

    @Test(expected = UserWithSuchUsernameAlreadyExist::class)
    fun createAlreadyExistUserTest() {
        userService.createUser(user)
        userService.createUser(user)
    }

    @Test
    fun findByUsernameTest() {
        userService.createUser(user)
        assertEquals(user, userService.findByUsername(user.username))
    }

    @Test(expected = UserNotFoundException::class)
    fun userNotFoundTest() {
        userService.createUser(user)
        userService.findByUsername("notExist")
    }

    @Test
    fun getAllUsersTest() {
        userService.createUser(user)
        userService.createUser(anotherUser)
        assertEquals(setOf(user, anotherUser), userService.getAllUsers())
    }

    @Test
    fun blockUserTest() {
        userService.createUser(user)
        val blockedUser = user.copy(blocked = true)
        assertEquals(blockedUser, userService.updateUser(blockedUser))
    }

    @Test(expected = PasswordNullOrEmptyException::class)
    fun updateToEmptyPasswordTest() {
        userService.createUser(user)
        val updatedUser = user.copy(password = "")
        assertEquals(updatedUser, userService.updateUser(updatedUser))
    }
}