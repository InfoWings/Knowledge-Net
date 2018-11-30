package com.infowings.catalog.auth

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.infowings.catalog.common.UserRole
import com.infowings.catalog.loggerFor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.firewall.DefaultHttpFirewall


@EnableWebSecurity
class WebSecurity : WebSecurityConfigurerAdapter() {

    @Bean
    fun allowUrlEncodedSlashHttpFirewall(): DefaultHttpFirewall {
        logger.info("Installing StrictHttpFirewall")
//        val firewall = StrictHttpFirewall()
//        firewall.setAllowSemicolon(true)
//        firewall.setAllowUrlEncodedSlash(true);
//        return firewall
        return DefaultHttpFirewall()
    }

    @Bean
    fun objectMapperBuilder(): Jackson2ObjectMapperBuilder =
        Jackson2ObjectMapperBuilder().modulesToInstall(KotlinModule())

    @Autowired
    lateinit var env: Environment

    @Autowired
    lateinit var userDetailsService: UserDetailsService

    @Autowired
    lateinit var jwtService: JWTService

    override fun configure(http: HttpSecurity?) {
        http?.sessionManagement()
            ?.sessionCreationPolicy(SessionCreationPolicy.STATELESS)

        val jWTAuthorizationFilter = JWTAuthorizationFilter(authenticationManager(), env, jwtService)

        http?.also {
            it.csrf().disable().authorizeRequests()
                .antMatchers("/api/access/signIn", "/api/access/refresh").permitAll()
                .antMatchers("/api/ping").permitAll()
                .antMatchers("/api/admin/**").hasAuthority(UserRole.ADMIN.name)
                .antMatchers("/api/powereduser/**").hasAuthority(UserRole.POWERED_USER.name)
                .antMatchers("/api/user/**").hasAuthority(UserRole.USER.name)
                .antMatchers("/api/aspect/**").hasAuthority(UserRole.POWERED_USER.name)
                .antMatchers("/api/aspect/**").hasAuthority(UserRole.ADMIN.name)
                .antMatchers("/api/measure/**").hasAuthority(UserRole.POWERED_USER.name)
                .antMatchers("/api/measure/**").hasAuthority(UserRole.ADMIN.name)
                .anyRequest().authenticated()
                .and()
                .addFilter(jWTAuthorizationFilter)
        }
    }

    override fun configure(web: WebSecurity?) {
        super.configure(web)
        web?.httpFirewall(allowUrlEncodedSlashHttpFirewall())
    }

    override fun configure(auth: AuthenticationManagerBuilder?) {
        auth?.userDetailsService(userDetailsService)
    }
}

private val logger = loggerFor<com.infowings.catalog.auth.WebSecurity>()
