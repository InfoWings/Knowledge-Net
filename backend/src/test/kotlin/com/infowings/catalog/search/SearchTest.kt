package com.infowings.catalog.search


import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.GlobalMeasureMap
import com.infowings.catalog.common.Metre
import com.infowings.catalog.data.Aspect
import com.infowings.catalog.data.AspectService
import com.infowings.catalog.loggerFor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private val logger = loggerFor<SearchTest>()

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SearchTest {

    @Autowired
    lateinit var suggestionService: SuggestionService;

    @Autowired
    lateinit var aspectService: AspectService

    @LocalServerPort
    lateinit var port: String

    @Value("\${spring.security.header.access}")
    lateinit var headerAcess: String

    @Value("\${spring.security.prefix}")
    lateinit var securityPrefix: String

    @Autowired
    private val wac: WebApplicationContext? = null

    private var mockMvc: MockMvc? = null

    @Before
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()
    }

    @Test
    fun measureSuggestion() {
        val queryText = "metre"
        val context = SearchContext()
        val res = suggestionService.findMeasure(context, queryText)

        logger.info("find result size: ${res.size}")
        assertFalse(res.isEmpty())

        logger.info("find result: $res")
        assertEquals("Metre", res.first().name)
        val m = GlobalMeasureMap[res.first().name]
        assertEquals(m, Metre)

        res.forEach { logger.info("name : ${it.name}") }
    }

    @Test
    fun aspectSuggestion() {
        val aspectName = "newAspectSuggestion"
        val aspect: Aspect = createTestAspect(aspectName)

        val res = suggestionService.findAspect(SearchContext(), aspectName)

        logger.info("find result size: ${res.size}")
        assertFalse(res.isEmpty())

        logger.info("find result: $res")

        assertEquals(aspectName, res.first().name)
        assertEquals(aspect.toAspectData(), res.first())
    }

    private fun createTestAspect(aspectName: String): Aspect {
        val ad = AspectData("", aspectName, null, null, null, emptyList())
        return aspectService.findByName(aspectName) ?: aspectService.createAspect(ad)
    }

    @Test
    fun measureSuggestionController() {
        mockMvc?.perform(
            get("/api/search/measure/suggestion?text=metr")
                .with(user("admin1").authorities(SimpleGrantedAuthority("ADMIN")))
        )?.andExpect(status().isOk)
            ?.andExpect(jsonPath("$[0]").value("Metre"))
    }

    @Test
    fun aspectSuggestionController() {
        val aspectName = "newAspectSuggestion"
        val aspect: Aspect = createTestAspect(aspectName)
        mockMvc?.perform(
            get("/api/search/aspect/suggestion?text=newAspectSuggestion")
                .with(user("admin").authorities(SimpleGrantedAuthority("ADMIN")))
        )?.andExpect(status().isOk)
            ?.andExpect(jsonPath("$[0].name").value(aspect.name))
    }

}