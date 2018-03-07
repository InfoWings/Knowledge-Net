package com.infowings.catalog.search


import com.infowings.catalog.AbstractMvcTest
import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.GlobalMeasureMap
import com.infowings.catalog.common.Metre
import com.infowings.catalog.data.Aspect
import com.infowings.catalog.data.AspectService
import com.infowings.catalog.loggerFor
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.context.WebApplicationContext
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private val logger = loggerFor<SearchTest>()

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
class SearchTest : AbstractMvcTest() {

    @Autowired
    lateinit var suggestionService: SuggestionService;

    @Autowired
    private val wac: WebApplicationContext? = null

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

    @Test
    fun findCycle() {
        val child = createTestAspectTree()
        var res = suggestionService.findAspectNoCycle(child.id, "level")
        assertEquals(1, res.size)
        assertEquals("level1_1", res[0].name)
        assertEquals(0, suggestionService.findAspectNoCycle(child.id, "level2").size)

        res = suggestionService.findParentAspects(child.id)
        assertEquals(3, res.size)
        assertEquals("level2", res[0].name)
        assertEquals("level1", res[1].name)
        assertEquals("root", res[2].name)
    }

    private fun createTestAspect(aspectName: String): Aspect {
        val ad = AspectData("", aspectName, null, null, null, emptyList())
        return aspectService.findByName(aspectName) ?: aspectService.createAspect(ad)
    }


    private fun createTestAspectTree(): Aspect {

        /*
         *  root
         *    level1_property
         *       level1
         *          level2_property
         *             level2
         *    level1_1_property
         *       level1_1
         */

        val level2_data = AspectData("", "level2", Kilogram.name, null, BaseType.Decimal.name, emptyList())
        val level2: Aspect = aspectService.createAspect(level2_data)
        val level2_property = AspectPropertyData("", "p_level2", level2.id, AspectPropertyPower.INFINITY.name)


        val level1_1_data = AspectData("", "level1_1", Kilogram.name, null, BaseType.Decimal.name, emptyList())
        val level1_1: Aspect = aspectService.createAspect(level1_1_data)
        val level1_1_property = AspectPropertyData("", "p_level1_1", level1_1.id, AspectPropertyPower.INFINITY.name)

        val level1_data = AspectData("", "level1", Kilometre.name, null, BaseType.Decimal.name, listOf(level2_property))
        val level1: Aspect = aspectService.createAspect(level1_data)
        val level1_property = AspectPropertyData("", "p_level1", level1.id, AspectPropertyPower.INFINITY.name)

        val ad = AspectData("", "root", Kilometre.name, null, BaseType.Decimal.name, listOf(level1_1_property, level1_property))
        val createAspect: Aspect = aspectService.createAspect(ad)


        val loaded = aspectService.findById(level2.id)
        return loaded
    }

    @Test
    fun measureSuggestionController() {
        mockMvc.perform(
            get("/api/search/measure/suggestion").with(authorities)
                .param("text", "metr")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0]").value("Metre"))
    }

    @Test
    fun aspectSuggestionController() {
        val aspect: Aspect = createTestAspect("newAspectSuggestion")
        mockMvc.perform(
            get("/api/search/aspect/suggestion").with(authorities)
                .param("text", aspect.name)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value(aspect.name))
    }

}