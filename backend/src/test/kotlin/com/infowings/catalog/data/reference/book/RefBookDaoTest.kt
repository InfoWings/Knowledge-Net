package com.infowings.catalog.data.reference.book

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.assertGreater
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.EventType
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.HistorySnapshot
import com.infowings.catalog.data.history.providers.SubjectHistoryProvider
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.search.SuggestionService
import com.infowings.catalog.storage.SUBJECT_CLASS
import com.infowings.catalog.storage.id
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assertEquals

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RefBookDaoTest {
    private val username = "admin"

    @Autowired
    lateinit var refBookDao: ReferenceBookDao

    @Autowired
    lateinit   var refBookService: ReferenceBookService

    @Autowired
    lateinit var aspectService: AspectService


    @Before
    fun initTestData() {
    }

    @Test
    fun testRefBookFindOne() {
        val aspectName = "aspect"
        val aspectDescr = "aspect description"
        val created = aspectService.save(AspectData(id = "", name = aspectName, description = aspectDescr,
            version = 0, deleted = false, baseType = BaseType.Text.name), username)
        val aspectId = created.id ?: throw IllegalStateException("aspect id is null")

        val rbName = "rb"
        val refBook = refBookService.createReferenceBook(rbName, aspectId, username)

        val refBookVertices = refBookDao.findStr(listOf(refBook.id))

        assertEquals(1, refBookVertices.size)

        val vertex = refBookVertices[0]
        assertEquals(refBook.id, vertex.id)
    }

    @Test
    fun testRefBookFindCorrectClass() {
        val aspectName = "aspect"
        val aspectDescr = "aspect description"
        val created = aspectService.save(AspectData(id = "", name = aspectName, description = aspectDescr,
            version = 0, deleted = false, baseType = BaseType.Text.name), username)
        val aspectId = created.id ?: throw IllegalStateException("aspect id is null")

        val rbName = "rb"
        val refBook = refBookService.createReferenceBook(rbName, aspectId, username)

        val refBookVertices = refBookDao.findStr(listOf(aspectId))

        assertEquals(0, refBookVertices.size)
    }
}