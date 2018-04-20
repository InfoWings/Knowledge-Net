package com.infowings.catalog.auth.user

import com.infowings.catalog.MasterCatalog
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

    @Before
    fun setUp() {
        // necessary to remove users created by initUsers method in OrientDatabaseInitializer class
        db.command("TRUNCATE CLASS $USER_CLASS UNSAFE") {}
        userService.createUser(user)
    }

    @Test
    fun createUserTest() {
        assertEquals(user, userService.createUser(user))
    }

    @Test
    fun findByUsernameTest() {
        assertEquals(user, userService.findByUsername(user.username))
    }

    @Test(expected = UsernameNotFoundException::class)
    fun userNotFoundTest() {
        userService.findByUsername("notExist")
    }

    @Test
    fun getAllUsersTest() {
        userService.createUser(anotherUser)
        assertEquals(setOf(user, anotherUser), userService.getAllUsers())
    }

    @Test
    fun blockUserTest() {
        assertEquals(user.copy(blocked = true), userService.blockUser(user.username))
    }
}