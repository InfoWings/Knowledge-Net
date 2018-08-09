package com.infowings.catalog.search

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.orientechnologies.orient.core.record.OVertex
import io.kotlintest.matchers.collections.shouldNotContain
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
class SearchWithRemovedTest {
    private val username = "admin"

    @Autowired
    lateinit var suggestionService: SuggestionService

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    lateinit var aspectDaoService: AspectDaoService

    @Autowired
    lateinit var database: OrientDatabase

    private lateinit var initialAspect: AspectData

    private lateinit var parentAspect: AspectData

    @BeforeEach
    fun saveAspectAndRemoveIt() {
        val ad = AspectData(null, randomName(), Metre.name, null, null)
        initialAspect = aspectService.save(ad, username)

        val p1 = AspectPropertyData("", "", initialAspect.idStrict(), PropertyCardinality.ONE.name, null)
        val ad2 = AspectData(null, randomName(), Tonne.name, null, null, listOf(p1))
        parentAspect = aspectService.save(ad2, username)

        session(database) {
            val aspectVertex = database.getVertexById(initialAspect.idStrict())!!.toAspectVertex()
            aspectVertex.deleted = true
            return@session aspectVertex.save<OVertex>()
        }
    }

    @Test
    fun `Search by text result must not contain deleted aspect`() {
        val byText = suggestionService.findAspect(SearchContext(), CommonSuggestionParam(text = initialAspect.name), null)
        byText.map { it.name }.shouldNotContain(initialAspect.name)
    }

    @Test
    fun `Search by measure text must not contain aspect`() {
        val byMeasureText = suggestionService.findAspect(SearchContext(), null, AspectSuggestionParam(measureText = Metre.name))
        byMeasureText.map { it.name }.shouldNotContain(initialAspect.name)
    }

    @Test
    fun `Search by measure name must not contain deleted aspect`() {
        val byMeasureName = suggestionService.findAspect(SearchContext(), null, AspectSuggestionParam(measureName = Metre.name))
        byMeasureName.map { it.name }.shouldNotContain(initialAspect.name)
    }

    @Test
    fun `Search parents must contain aspect and his parent`() {
        aspectDaoService.findParentAspects(initialAspect.idStrict()).size shouldBe 2
    }
}