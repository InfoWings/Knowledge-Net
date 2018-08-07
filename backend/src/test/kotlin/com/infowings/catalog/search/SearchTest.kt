package com.infowings.catalog.search


import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.loggerFor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.annotation.DirtiesContext
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
import kotlin.test.assertTrue

private val logger = loggerFor<SearchTest>()

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SearchTest {
    private val username = "admin"

    @Autowired
    lateinit var suggestionService: SuggestionService

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    lateinit var aspectDaoService: AspectDaoService

    @Autowired
    lateinit var wac: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    private val authorities = user("admin").authorities(SimpleGrantedAuthority("ADMIN"))

    @Before
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()
    }

    @Test
    fun measureSuggestion() {
        val queryText = "metre"
        val res = suggestionService.findMeasure(CommonSuggestionParam(text = queryText), null)

        logger.debug("find result size: ${res.size}")
        assertFalse(res.isEmpty())

        logger.debug("find result: $res")
        assertEquals("Metre", res.first().name)
        val m = GlobalMeasureMap[res.first().name]
        assertEquals(m, Metre)

        res.forEach { logger.debug("name : ${it.name}") }
    }

    @Test
    fun measureSuggestionByDesc() {
        val queryText = "charge is the physical property"
        val res = suggestionService.findMeasure(CommonSuggestionParam(text = queryText), null)

        logger.debug("find result size: ${res.size}")
        assertFalse(res.isEmpty())

        logger.debug("find result: $res")
        assertEquals("Coulomb", res.first().name)
        val m = GlobalMeasureMap[res.first().name]
        assertEquals(m, Coulomb)

        res.forEach { logger.debug("name : ${it.name}") }
    }

    @Test
    fun mmSuggestion() {
        val queryText = "mm"
        val res = suggestionService.findMeasure(CommonSuggestionParam(text = queryText), null)

        logger.debug("find result size: ${res.size}")
        assertFalse(res.isEmpty())

        logger.debug("find result: $res")
        val m = GlobalMeasureMap[res.first().name]
        assertEquals(m, Millimetre)

        res.forEach { logger.debug("name : ${it.name}") }
    }

    @Test
    fun measureSuggestionWithGroup() {
        val queryText = "Are"
        val result = suggestionService.findMeasure(
            CommonSuggestionParam(text = queryText),
            measureGroupName = null,
            findInGroups = true
        )

        assertTrue(result.measureNames.contains("Square meter"))
        assertTrue(result.measureGroupNames.contains("Area"))
    }

    @Test
    fun measureSuggestionInGroup() {
        val queryText = "metre"
        val res = suggestionService.findMeasure(CommonSuggestionParam(text = queryText), "Length")

        logger.debug("find result size: ${res.size}")
        assertFalse(res.isEmpty())

        logger.debug("find result: $res")
        assertEquals("Metre", res.first().name)
        val m = GlobalMeasureMap[res.first().name]
        assertEquals(m, Metre)

        res.forEach { logger.debug("name : ${it.name}") }
    }

    @Test
    fun aspectSuggestion() {
        val aspectName = "aspectSuggestionTst"
        val aspect: AspectData = createTestAspect(aspectName)

        val res = suggestionService.findAspect(SearchContext(), CommonSuggestionParam(text = aspectName), null)

        logger.debug("find result size: ${res.size}")
        assertFalse("result set cannot by empty!") { res.isEmpty() }

        logger.debug("find result: $res")

        assertEquals(aspectName, res.first().name)
        assertEquals(aspect, res.first())
    }

    @Test
    fun aspectDescSuggestion() {
        val aspectName = "descAspect"
        val aspect: AspectData = createTestAspect(
            aspectName,
            "Aspect is a grammatical category that expresses how an action, event, or state, denoted by a verb, extends over time. Perfective aspect is used in referring to an event conceived as bounded and unitary, without reference to any flow of time during (\"I helped him\"). Imperfective aspect is used for situations conceived as existing continuously or repetitively as time flows "
        )

        val res = suggestionService.findAspect(
            SearchContext(),
            CommonSuggestionParam(text = "grammatical category that expresses"),
            null
        )

        logger.debug("find result size: ${res.size}")
        assertFalse("result set cannot by empty!") { res.isEmpty() }

        logger.debug("find result: $res")

        assertEquals(aspectName, res.first().name)
        assertEquals(aspect, res.first())
    }


    @Test
    fun findCycle() {
        val child = createTestAspectTree()
        var res = suggestionService.findAspectNoCycle(child.idStrict(), "level")
        assertEquals(1, res.size)
        assertEquals("level1_1", res[0].name)
        assertEquals(0, suggestionService.findAspectNoCycle(child.idStrict(), "level2").size)

        res = aspectDaoService.findParentAspects(child.idStrict())
        assertEquals(3, res.size)
        assertEquals("level2", res[0].name)
        assertEquals("level1", res[1].name)
        assertEquals("root", res[2].name)
    }

    private fun createTestAspect(aspectName: String, desc: String? = null): AspectData {
        val ad = AspectData("", aspectName, Metre.name, null, null, emptyList(), description = desc)
        return aspectService.findByName(aspectName).firstOrNull() ?: aspectService.save(ad, username)
    }


    private fun createTestAspectTree(): AspectData {

        /*
         *  root
         *    level1_property
         *       level1
         *          level2_property
         *             level2
         *    level1_1_property
         *       level1_1
         */

        val level2Data = AspectData(
            "",
            "level2",
            Kilogram.name,
            null,
            BaseType.Decimal.name,
            emptyList()
        )
        val level2: AspectData = aspectService.save(level2Data, username)
        val level2Property = AspectPropertyData("", "p_level2", level2.idStrict(), PropertyCardinality.INFINITY.name, null)


        val level11Data = AspectData(
            "",
            "level1_1",
            Kilogram.name,
            null,
            BaseType.Decimal.name,
            emptyList()
        )
        val level11: AspectData = aspectService.save(level11Data, username)
        val level11Property =
            AspectPropertyData("", "p_level1_1", level11.idStrict(), PropertyCardinality.INFINITY.name, null)

        val level1Data = AspectData(
            "",
            "level1",
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            listOf(level2Property)
        )
        val level1: AspectData = aspectService.save(level1Data, username)
        val level1Property = AspectPropertyData("", "p_level1", level1.idStrict(), PropertyCardinality.INFINITY.name, null)

        val ad = AspectData(
            "",
            "root",
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            listOf(level11Property, level1Property)
        )
        aspectService.save(ad, username)


        return aspectService.findById(level2.idStrict())
    }

    @Test
    fun measureSuggestionController() {
        mockMvc.perform(
            get("/api/search/measure/suggestion").with(authorities)
                .param("text", "metr")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$['measureNames'][0]").value("Metre"))
    }

    @Test
    fun aspectSuggestionController() {
        val aspect: AspectData = createTestAspect("newAspectSuggestion")
        mockMvc.perform(
            get("/api/search/aspect/suggestion").with(authorities)
                .param("text", aspect.name)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$['aspects'][0].name").value(aspect.name))
    }

}