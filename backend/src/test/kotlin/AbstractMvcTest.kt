package com.infowings.catalog

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.data.Aspect
import com.infowings.catalog.data.AspectService
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
abstract class AbstractMvcTest {

    @Autowired
    private lateinit var aspectService: AspectService

    @Autowired
    private val wac: WebApplicationContext? = null

    protected lateinit var mockMvc: MockMvc

    protected val authorities = user("admin").authorities(SimpleGrantedAuthority("ADMIN"))

    @Before
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()
    }

    protected fun createTestAspect(aspectName: String): Aspect {
        val ad = AspectData("", aspectName, null, null, null, emptyList())
        return aspectService.findByName(aspectName) ?: aspectService.createAspect(ad)
    }

}