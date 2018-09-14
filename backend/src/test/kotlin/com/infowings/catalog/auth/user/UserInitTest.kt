package com.infowings.catalog.auth.user

import com.infowings.catalog.AbstractDatabaseTest
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.transaction
import io.kotlintest.inspectors.forExactly
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired

@Suppress("UnsafeCallOnNullableType")
class UserInitTest : AbstractDatabaseTest() {

    @Autowired
    lateinit var userDao: UserDao

    @Test
    fun `Database initializer should not change the database if properties do not have new user`() {
        val userVertexes = transaction(orientDatabase) {
            userDao.getAllUserVertices().toList()
        }
        val firstUserVertex = userVertexes.first()

        val userProperties = UserProperties().apply {
            user.add(createUserConfig(firstUserVertex.username, firstUserVertex.password, firstUserVertex.role))
        }

        UserDatabaseInitializer(orientDatabase, userDao).initUsers(userProperties)

        val firstUserVertexAfterInit = transaction(orientDatabase) {
            userDao.findByUsername(firstUserVertex.username)!!
        }

        assertAll(
            "All fields before and after initialization should be equal",
            { firstUserVertex.id       shouldBe firstUserVertexAfterInit.id },
            { firstUserVertex.username shouldBe firstUserVertexAfterInit.username },
            { firstUserVertex.password shouldBe firstUserVertexAfterInit.password },
            { firstUserVertex.role     shouldBe firstUserVertexAfterInit.role }
        )
    }

    @Test
    fun `Database initializer should add new user if it is not present in database`() {
        val username = "notAdmin"
        val password = "notAdmin"
        val role = "ADMIN"

        val userProperties = UserProperties().apply {
            user.add(createUserConfig(username, password, role))
        }

        UserDatabaseInitializer(orientDatabase, userDao).initUsers(userProperties)

        val userVertexes = transaction(orientDatabase) {
            userDao.getAllUserVertices().toList()
        }

        userVertexes.forExactly(1) {
            it.username shouldBe username
            it.password shouldBe password
            it.role     shouldBe role
        }
    }

    @Test
    fun `Database initializer should change password for user with the same username`() {
        val userVertexes = transaction(orientDatabase) {
            userDao.getAllUserVertices().toList()
        }
        val firstUserVertex = userVertexes.first()
        val newPassword = firstUserVertex.password + "suffix"

        val userProperties = UserProperties().apply {
            user.add(createUserConfig(firstUserVertex.username, newPassword, firstUserVertex.role))
        }

        UserDatabaseInitializer(orientDatabase, userDao).initUsers(userProperties)

        val firstUserVertexAfterInit = transaction(orientDatabase) {
            userDao.findByUsername(firstUserVertex.username)!!
        }

        assertAll(
            "All fields before and after initialization should be equal",
            { firstUserVertexAfterInit.username shouldBe firstUserVertex.username },
            { firstUserVertexAfterInit.password shouldBe newPassword },
            { firstUserVertexAfterInit.role     shouldBe firstUserVertex.role }
        )
    }

    private fun createUserConfig(username: String, password: String, role: String): UserConfig {
        return UserConfig().apply {
            this.username = username
            this.password = password
            this.role = role
        }
    }
}