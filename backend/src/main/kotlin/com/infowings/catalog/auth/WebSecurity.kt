package com.infowings.catalog.auth

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.infowings.common.UserRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.UserDetailsService


@EnableWebSecurity
class WebSecurity() : WebSecurityConfigurerAdapter() {

    @Bean
    fun objectMapperBuilder(): Jackson2ObjectMapperBuilder = Jackson2ObjectMapperBuilder().modulesToInstall(KotlinModule())

    @Autowired
    lateinit var env: Environment

    @Autowired
    lateinit var userDetailsService: UserDetailsService

    @Autowired
    lateinit var jwtService: JWTService

    override fun configure(http: HttpSecurity?) {

        val jWTAuthorizationFilter = JWTAuthorizationFilter(authenticationManager(), env, jwtService)

        http?.let {
            it.csrf().disable().authorizeRequests()
                    .antMatchers("/api/access/signIn", "/api/access/refresh").permitAll()
                    .antMatchers("/api/admin/**").hasAuthority(UserRole.ADMIN.name)
                    .antMatchers("/api/powereduser/**").hasAuthority(UserRole.POWERED_USER.name)
                    .antMatchers("/api/user/**").hasAuthority(UserRole.USER.name)
                    .anyRequest().authenticated()
                    .and()
                    .addFilter(jWTAuthorizationFilter)
        }
    }

    override fun configure(auth: AuthenticationManagerBuilder?) {
        auth?.userDetailsService(userDetailsService)
    }
}