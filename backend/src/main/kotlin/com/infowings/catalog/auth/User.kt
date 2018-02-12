package com.infowings.catalog.auth

import com.infowings.common.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import javax.persistence.*

@Entity
@Table(name = "userentity", indexes = [Index(name = "username_ind", columnList = "username", unique = true)])
data class UserEntity(@Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
                      @Column(name = "username")
                      var username: String,
                      var password: String,
                      @Enumerated(EnumType.STRING) var role: UserRole)

interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByUsername(username: String): UserEntity?
}