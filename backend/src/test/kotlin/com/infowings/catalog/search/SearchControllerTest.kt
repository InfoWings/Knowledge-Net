package com.infowings.catalog.search

import com.infowings.catalog.MasterCatalog
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.greaterThan
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner


@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
class SearchControllerTest {

    @Test
    fun measureSuggestion() {
        given().`when`()
                .get("/api/search/measure/suggestion")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
    }
}