package com.infowings.catalog.search

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.*
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.orientechnologies.orient.core.record.OVertex
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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

    private lateinit var initialAspect: Aspect

    private lateinit var parentAspect: Aspect

    @Before
    fun saveAspectAndRemoveIt() {
        val ad = AspectData(null, "aspect1", Metre.name, null, null)
        initialAspect = aspectService.save(ad, username)

        val p1 = AspectPropertyData("", "", initialAspect.id, PropertyCardinality.ONE.name)
        val ad2 = AspectData(null, "aspect2", Tonne.name, null, null, listOf(p1))
        parentAspect = aspectService.save(ad2, username)

        session(database) {
            val aspectVertex = database.getVertexById(initialAspect.id)!!.toAspectVertex()
            aspectVertex.deleted = true
            return@session aspectVertex.save<OVertex>()
        }
    }

    @Test
    fun testSearchDeletedAspect() {
        val byText =
            suggestionService.findAspect(SearchContext(), CommonSuggestionParam(text = initialAspect.name), null)

        assertThat(
            "Search by text result not contain aspect ${initialAspect.name}",
            byText[0].name,
            Is.`is`(parentAspect.name)
        )

        val byMeasureName =
            suggestionService.findAspect(SearchContext(), null, AspectSuggestionParam(measureName = Metre.name))

        assertThat(
            "Search by measure name must not contain aspect ${initialAspect.name}",
            byMeasureName.map { it.name }.contains(initialAspect.name),
            Is.`is`(false)
        )

        val byMeasureText =
            suggestionService.findAspect(SearchContext(), null, AspectSuggestionParam(measureText = Metre.name))

        assertThat(
            "Search by measure text must not contain aspect ${initialAspect.name}",
            byMeasureText.isEmpty(),
            Is.`is`(true)
        )
    }

    @Test
    fun testFindAsParent() {

        val searched = aspectDaoService.findParentAspects(initialAspect.id)

        assertThat(
            "Search parents must contain aspect and his parent",
            searched.size,
            Is.`is`(2)
        )
    }
}