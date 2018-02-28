package com.infowings.catalog.search


import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.GlobalMeasureMap
import com.infowings.catalog.common.Metre
import com.infowings.catalog.data.Aspect
import com.infowings.catalog.data.AspectService
import com.infowings.catalog.loggerFor
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.BeforeTest
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

    @BeforeTest
    fun beforeTest() {
        RestAssured.baseURI = "http://localhost:$port"
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
        val response = given().log().all()
            .contentType("application/json")
            .header(headerAcess, signIn())
            .`when`()
            .get(
                "/api/search/measure/suggestion?text=metr" +
                        "&aspects=aspectTest1" +
                        "&aspects=aspectTest2"
            )

        logger.info("measure suggestion result: ${response.body.print()}")

        response.then()
            .statusCode(200)
            .body("[0]", equalTo("Metre"))
    }

    @Test
    fun aspectSuggestionController() {
        val aspectName = "newAspectSuggestion"
        val aspect: Aspect = createTestAspect(aspectName)
        val response = given().log().all()
            .contentType("application/json")
            .header(headerAcess, signIn())
            .`when`()
            .get(
                "/api/search/aspect/suggestion?text=newAspectSuggestion" +
                        "&aspects=aspectTest1" +
                        "&aspects=aspectTest2"
            )

        logger.info("aspect suggestion result: ${response.body.print()}")
        response.then()
            .statusCode(200)
            .body("[0].name", equalTo(aspect.name))
    }

    private fun signIn(): String {
        val response = given().log().all()
            .contentType("application/json")
            .body("{\"username\":\"admin\",\"password\":\"admin\"}")
            .`when`()
            .post("/api/access/signIn")

        response.then().statusCode(200)
        logger.info("signIn result: ${response.body.asString()}")

        return securityPrefix + " " + response.jsonPath().get("accessToken")
    }
}