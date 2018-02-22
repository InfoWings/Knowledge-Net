package com.infowings.catalog.search


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
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private val logger = loggerFor<SearchTest>()

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
class SearchTest {

    @Autowired
    lateinit var suggestionService: SuggestionService;

    @Autowired
    lateinit var aspectService: AspectService

    @Test
    fun measureSuggestion() {
        val queryText = "metre"
        val context = SearchContext(emptyList(), emptyList())
        val res = suggestionService.findMeasure(context, queryText)

        logger.info("find result size: ${res.size}")
        assertFalse(res.isEmpty())

        logger.info("find result: $res")
        assertEquals("Metre",  res.first().name)
        val m = GlobalMeasureMap[res.first().name]
        assertEquals(m, Metre)

        res.map { it.name }
                .forEach {
                    logger.info("name : $it")
                }
    }

    @Test
    fun aspectSuggestion() {
        val aspectName = "newAspectSuggestion"
        val ad = AspectData("", aspectName, null, null, null, emptyList())
        val aspect: Aspect = aspectService.findByName(aspectName) ?: aspectService.createAspect(ad)

        val context = SearchContext(emptyList(), emptyList())
        val res = suggestionService.findAspect(context, aspectName)

        logger.info("find result size: ${res.size}")
        assertFalse(res.isEmpty())

        logger.info("find result: $res")

        assertEquals(aspectName, res.first().name)
        assertEquals(aspect.toAspectData(), res.first())
    }
}