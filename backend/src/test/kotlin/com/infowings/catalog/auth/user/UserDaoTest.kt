package com.infowings.catalog.auth.user

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.USER_CLASS
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
class UserDaoTest {
    @Autowired
    private lateinit var db: OrientDatabase

    @Autowired
    private lateinit var userDao: UserDao

    private lateinit var userVertex: UserVertex

    private val username = "test"

    @Before
    fun setUp() {
        // necessary to remove users created by initUsers method in OrientDatabaseInitializer class
        db.command("TRUNCATE CLASS $USER_CLASS UNSAFE") {}

        userVertex = userDao.createUserVertex()
        userVertex.username = username
        userVertex.password = "qwerty123"
        userVertex.role = "someRole"
        userDao.saveUserVertex(userVertex)
    }

    @Test
    fun createUserVertexTest() {
        val userVertex = userDao.createUserVertex()

        assertEquals(UserVertex::class, userVertex::class)
        assertNull(userVertex.username)
        assertNull(userVertex.password)
        assertNull(userVertex.role)
    }

    @Test
    fun saveUserVertexTest() {
        assertEquals(userVertex, userDao.saveUserVertex(userVertex))
    }

    @Test
    fun findByUsernameTest() {
        assertEquals(userVertex, userDao.findByUsername(username))
    }

    @Test
    fun findNotExistByUsernameTest() {
        assertNull(userDao.findByUsername("notExist"))
    }

    @Test
    fun getAllUserVerticesTest() {
        val anotherUserVertex = userDao.createUserVertex()
        anotherUserVertex.username = "another"
        anotherUserVertex.password = "123456"
        anotherUserVertex.role = "anotherRole"
        userDao.saveUserVertex(anotherUserVertex)

        assertEquals(setOf(userVertex, anotherUserVertex), userDao.getAllUserVertices())
    }
}