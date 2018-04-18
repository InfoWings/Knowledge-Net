package com.infowings.catalog.auth.user

import org.springframework.security.core.userdetails.UsernameNotFoundException

class UserService(private val dao: UserDao) {
    fun saveUser(user: UserEntity) =
        dao.saveUser(user)

    fun findByUsername(username: String): UserEntity {
        val userVertex = findUserVertexByUsername(username)
        return userVertex.toUserEntity()
    }

    fun findByUsernameAsJson(username: String): String {
        val userVertex = findUserVertexByUsername(username)
        return userVertex.toJSON()
    }

    private fun findUserVertexByUsername(username: String) =
        dao.findByUsername(username) ?: throw UsernameNotFoundException(username)
}
