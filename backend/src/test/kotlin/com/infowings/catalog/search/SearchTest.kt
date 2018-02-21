package com.infowings.catalog.search


import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.loggerFor
import com.infowings.common.Measure
import com.infowings.common.Metre
import com.infowings.common.search.SearchContext
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val logger = loggerFor<SearchTest>()

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
class SearchTest {

    @Autowired
    lateinit var suggestionService: SuggestionService;

    @Test
    fun measureSuggestion() {
        val queryText = "metr"
        val context = SearchContext(emptyList(), emptyList())
        val res = suggestionService.find(context, queryText)

        logger.info("find result size: ${res.size}")
        assertFalse(res.isEmpty())

        logger.info("find result: $res")
        assertEquals("Metre",  res.first().name)
        assertEquals(Measure[res.first().name], Metre)

        res.map { it.name }
                .forEach {
                    logger.info("name : $it")
                    assertTrue { it.toLowerCase().contains(queryText) }
                }
    }
}