package com.infowings.catalog.search

import com.infowings.catalog.MasterCatalog
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
class SearchControllerTest {

    @Autowired
    lateinit var suggestionService: SuggestionService;

    @Test
    fun measureSuggestion() {
        val queryText = "metr"
        val res = suggestionService.find("", queryText)

        println("find result size: ${res.size}")
        assertFalse(res.isEmpty())

        println("find result: $res")
        assertEquals("Metre", res.first().name)

        res.map { it.name }
                .forEach {
                    println("name : $it")
                    assertTrue { it.toLowerCase().contains(queryText) }
                }
    }
}