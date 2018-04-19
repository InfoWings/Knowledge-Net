package com.infowings.catalog.data.reference.book

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.Metre
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.data.aspect.Aspect
import com.infowings.catalog.data.aspect.AspectService
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReferenceBookDaoTest {
    @Autowired
    private lateinit var dao: ReferenceBookDao
    @Autowired
    private lateinit var aspectService: AspectService
    @Autowired
    private lateinit var referenceBookService: ReferenceBookService

    private lateinit var aspect: Aspect

    private val username = "admin"

    @Before
    fun initTestData() {
        aspect = aspectService.save(AspectData("", "aspect", Metre.name, null, null), username)
    }

    //todo: after Object entity will added this test could be moved to ReferenceBookDaoTest
    @Test
    fun fakeRemoveTest() {
        val book1 = referenceBookService.createReferenceBook("book1", aspect.id, username)
        val anotherAspect = aspectService.save(AspectData("", "anotherAspect", Metre.name, null, null), username)
        val anotherAspectId = anotherAspect.id
        val book2 = referenceBookService.createReferenceBook("book2", anotherAspectId, username)
        val item1 = createReferenceBookItem(anotherAspectId, book2.id, "v1")
        val idItem1 = referenceBookService.addReferenceBookItem(item1, username)
        val item11 = createReferenceBookItem(anotherAspectId, idItem1, "v2")
        val idItem11 = referenceBookService.addReferenceBookItem(item11, username)
        dao.markBookVertexAsDeleted(
            dao.getReferenceBookVertex(anotherAspectId) ?: throw RefBookNotExist(anotherAspectId)
        )
        assertEquals(listOf(book1), referenceBookService.getAllReferenceBooks())
        assertTrue(referenceBookService.getReferenceBookItem(idItem1).deleted)
        assertTrue(referenceBookService.getReferenceBookItem(idItem11).deleted)
    }

    private fun createReferenceBookItem(
        aspectId: String,
        parentId: String,
        value: String
    ): ReferenceBookItem {
        return ReferenceBookItem(
            aspectId,
            parentId,
            "",
            value,
            emptyList(),
            false,
            0
        )
    }
}