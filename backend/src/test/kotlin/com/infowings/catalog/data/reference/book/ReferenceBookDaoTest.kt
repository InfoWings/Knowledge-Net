package com.infowings.catalog.data.reference.book

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BaseType
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
        aspect = aspectService.save(AspectData("", "aspect", null, null, BaseType.Text.name), username)
    }

    //todo: after Object entity will added this test could be moved to ReferenceBookDaoTest
    @Test
    fun markReferenceBookAsRemovedTest() {
        val book1 = referenceBookService.createReferenceBook("book1", aspect.id, username)
        val anotherAspect = aspectService.save(AspectData("", "anotherAspect", null, null, BaseType.Text.name), username)
        val anotherAspectId = anotherAspect.id
        referenceBookService.createReferenceBook("book2", anotherAspectId, username)
        val item1 = createReferenceBookItem("v1")
        val idItem1 = referenceBookService.addReferenceBookItem(anotherAspectId, item1, username)
        val item11 = createReferenceBookItem("v2")
        val idItem11 = referenceBookService.addReferenceBookItem(idItem1, item11, username)
        dao.markBookVertexAsDeleted(
            dao.getReferenceBookVertex(anotherAspectId) ?: throw RefBookNotExist(anotherAspectId)
        )
        assertEquals(listOf(book1), referenceBookService.getAllReferenceBooks())
        assertTrue(referenceBookService.getReferenceBookItem(idItem1).deleted)
        assertTrue(referenceBookService.getReferenceBookItem(idItem11).deleted)
    }

    private fun createReferenceBookItem(value: String): ReferenceBookItem {
        return ReferenceBookItem(
            "",
            value,
            emptyList(),
            false,
            0
        )
    }
}