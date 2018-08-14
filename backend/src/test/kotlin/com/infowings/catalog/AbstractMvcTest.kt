package com.infowings.catalog

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.toSubjectData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@ExtendWith(SpringExtension::class)
@SpringBootTest
abstract class AbstractMvcTest {

    @Autowired
    private lateinit var aspectService: AspectService

    @Autowired
    private lateinit var wac: WebApplicationContext

    protected lateinit var mockMvc: MockMvc

    protected val authorities = user("admin").authorities(SimpleGrantedAuthority("ADMIN"))

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.print())
            .build()
    }

}

fun createTestAspect(
    aspectName: String,
    aspectService: AspectService,
    subject: Subject? = null
): AspectData {
    // Если есть аспект с заданным aspectName и subject.id - возвращаем его.
    // Если нет - создаем

    val ad = AspectData(
        "",
        aspectName,
        "Metre",
        null,
        null,
        emptyList(),
        subject = subject?.toSubjectData()
    )
    return aspectService.findByName(aspectName)
        .find { it.subject?.id == subject?.id } ?: aspectService.save(ad, "admin")
}