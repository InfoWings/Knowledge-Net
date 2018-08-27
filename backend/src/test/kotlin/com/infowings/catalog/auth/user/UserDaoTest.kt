package com.infowings.catalog.auth.user

import com.infowings.catalog.common.UserRole
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.OrientDatabase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserDaoTest {
    @Autowired
    private lateinit var db: OrientDatabase

    @Autowired
    private lateinit var userDao: UserDao

    private lateinit var userVertex: UserVertex

    @Suppress("JoinDeclarationAndAssignment")
    private lateinit var username: String

    init {
        username = randomName()
    }

    @BeforeEach
    fun setUp() {
        // necessary to remove users created by initUsers method in OrientDatabaseInitializer class
  //      db.command("TRUNCATE CLASS $USER_CLASS UNSAFE") {}

        userVertex = userDao.createUserVertex()
        userVertex.username = username
        userVertex.password = "qwerty123"
        userVertex.role = UserRole.POWERED_USER.toString()
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
        anotherUserVertex.role = UserRole.USER.toString()
        userDao.saveUserVertex(anotherUserVertex)

        val allVertices = userDao.getAllUserVertices()
        assert(allVertices.size >= 2)
        assertEquals(allVertices.filter { it.username == "another"}, listOf(anotherUserVertex))
        assertEquals(allVertices.filter { it.username == username}, listOf(userVertex))
    }

}