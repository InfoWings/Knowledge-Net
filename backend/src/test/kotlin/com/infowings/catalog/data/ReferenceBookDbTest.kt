package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.ReferenceBook
import org.junit.Assert.assertTrue
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
class ReferenceBookDbTest {

    @Autowired
    private lateinit var referenceBookService: ReferenceBookService
    @Autowired
    private lateinit var aspectService: AspectService

    private lateinit var aspect: Aspect
    private lateinit var referenceBook: ReferenceBook

    @Before
    fun initTestData() {
        aspect = aspectService.save(AspectData("", "aspect", null, null, null))
        referenceBook = referenceBookService.createReferenceBook("Example", aspect.id)
    }

    @Test
    fun saveReferenceBookTest() {
        assertTrue("Saved reference book must contains generated id", referenceBook.name == "Example")
    }

    @Test
    fun findReferenceBookTest() {
        val found = referenceBookService.getReferenceBook("Example")
        assertTrue("Found reference book must be equals with saved", found == referenceBook)
    }

    @Test(expected = RefBookNotExist::class)
    fun findNotExistingReferenceBookTest() {
        referenceBookService.getReferenceBook("random")
    }

    @Test
    fun addReferenceBookItemAsAChildToExistingItemTest() {
        val newId = referenceBookService.addReferenceBookItem(referenceBook.id, "value")
        assertTrue("New item was created", referenceBookService.getReferenceBookItem(newId).value == "value")
    }

    @Test
    fun addChildrenTest() {
        val child1 = referenceBookService.addReferenceBookItem(referenceBook.id, "value1")
        referenceBookService.addReferenceBookItem(referenceBook.id, "value2")
        val child11 = referenceBookService.addReferenceBookItem(child1, "value11")
        referenceBookService.addReferenceBookItem(child11, "value111")

        val updatedReferenceBook = referenceBookService.getReferenceBook(referenceBook.name)
        assertTrue("Root has 2 children", updatedReferenceBook.children.size == 2)
        assertTrue("`root.value1` has 1 child", updatedReferenceBook["value1"]!!.children.size == 1)
        assertTrue("`root.value1.value11` has 1 child", updatedReferenceBook["value1"]!!["value11"]!!.children.size == 1)
    }

    @Test(expected = RefBookChildAlreadyExist::class)
    fun addChildrenWithSameValueAsOtherChildrenTest() {
        referenceBookService.addReferenceBookItem(referenceBook.id, "value1")
        referenceBookService.addReferenceBookItem(referenceBook.id, "value1")
    }

    @Test
    fun correctMoveItemsTest() {
        val child1 = referenceBookService.addReferenceBookItem(referenceBook.id, "value1")
        val child2 = referenceBookService.addReferenceBookItem(referenceBook.id, "value2")
        val child11 = referenceBookService.addReferenceBookItem(child1, "value11")
        referenceBookService.moveReferenceBookItem(child11, child2)

        val updatedReferenceBook = referenceBookService.getReferenceBook(referenceBook.name)
        assertTrue("`root.value1` has no child", updatedReferenceBook["value1"]!!.children.isEmpty())
        assertTrue("`root.value2` has 1 child", updatedReferenceBook["value2"]!!.children.size == 1)
    }

    @Test(expected = RefBookItemMoveImpossible::class)
    fun unCorrectMoveItemsTest() {
        val child1 = referenceBookService.addReferenceBookItem(referenceBook.id, "value1")
        val child11 = referenceBookService.addReferenceBookItem(child1, "value11")
        referenceBookService.moveReferenceBookItem(child1, child11)
    }

    @Test
    fun correctChangeValueTest() {
        val childId = referenceBookService.addReferenceBookItem(referenceBook.id, "value1")
        referenceBookService.changeValue(childId, "value2")
        val updated = referenceBookService.getReferenceBookItem(childId)
        assertTrue("Value should be changed", updated.value == "value2")
    }

    @Test(expected = RefBookChildAlreadyExist::class)
    fun unCorrectChangeValueTest() {
        val childId = referenceBookService.addReferenceBookItem(referenceBook.id, "value1")
        referenceBookService.addReferenceBookItem(referenceBook.id, "value2")
        referenceBookService.changeValue(childId, "value2")
    }
}