package com.infowings.catalog.search


import com.infowings.catalog.AbstractMvcTest
import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.GlobalMeasureMap
import com.infowings.catalog.common.Metre
import com.infowings.catalog.data.Aspect
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