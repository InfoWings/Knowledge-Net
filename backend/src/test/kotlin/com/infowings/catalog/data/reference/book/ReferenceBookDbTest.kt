package com.infowings.catalog.data.reference.book

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.Metre
import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.data.aspect.Aspect
import com.infowings.catalog.data.aspect.AspectService
import org.hamcrest.core.Is
import org.junit.Assert.*
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
        aspect = aspectService.save(AspectData("", "aspect", Metre.name, null, null))
        referenceBook = referenceBookService.createReferenceBook("Example", aspect.id)
    }

    @Test
    fun testNotVirtualId() {
        assertThat(
            "Ids are not virtual",
            referenceBookService.getReferenceBook(referenceBook.aspectId).id.contains("-"),
            Is.`is`(false)
        )
    }

    @Test
    fun saveReferenceBookTest() {
        assertTrue("Saved reference book must contains generated id", referenceBook.name == "Example")
    }

    @Test
    fun getAllReferenceBooksTest() {
        val anotherAspect = aspectService.save(AspectData("", "anotherAspect", Metre.name, null, null))
        val anotherBook = referenceBookService.createReferenceBook("Example", anotherAspect.id)
        assertEquals(referenceBookService.getAllReferenceBooks().toSet(), setOf(anotherBook, referenceBook))
    }

    @Test
    fun findReferenceBookTest() {
        assertEquals(referenceBookService.getReferenceBook(aspect.id), referenceBook)
    }

    @Test(expected = RefBookNotExist::class)
    fun findNotExistingReferenceBookTest() {
        referenceBookService.getReferenceBook("random")
    }

    @Test
    fun updateReferenceBookTest() {
        val newName = "newName"
        val updatedReferenceBook = referenceBookService.updateReferenceBook(referenceBook.aspectId, newName)
        assertEquals(referenceBook.copy(name = newName), updatedReferenceBook)
    }

    @Test(expected = RefBookNotExist::class)
    fun updateNotExistReferenceBookTest() {
        referenceBookService.updateReferenceBook("random", "newName")
    }

    @Test
    fun addReferenceBookItemAsAChildToExistingItemTest() {
        val newId = referenceBookService.addReferenceBookItem(referenceBook.aspectId, referenceBook.id, "value")
        assertTrue("New item was created", referenceBookService.getReferenceBookItem(newId).value == "value")
    }

    @Test
    fun addChildrenTest() {
        val aspectId = referenceBook.aspectId
        val parentId = referenceBook.id
        val child1 = referenceBookService.addReferenceBookItem(aspectId, parentId, "value1")
        referenceBookService.addReferenceBookItem(aspectId, parentId, "value2")
        val child11 = referenceBookService.addReferenceBookItem(aspectId, child1, "value11")
        referenceBookService.addReferenceBookItem(aspectId, child11, "value111")

        val updatedReferenceBook = referenceBookService.getReferenceBook(aspectId)
        assertTrue("Root has 2 children", updatedReferenceBook.children.size == 2)
        assertTrue("`root.value1` has 1 child", updatedReferenceBook["value1"]!!.children.size == 1)
        assertTrue(
            "`root.value1.value11` has 1 child",
            updatedReferenceBook["value1"]!!["value11"]!!.children.size == 1
        )
    }

    @Test(expected = RefBookChildAlreadyExist::class)
    fun addChildrenWithSameValueAsOtherChildrenTest() {
        val aspectId = referenceBook.aspectId
        val parentId = referenceBook.id
        referenceBookService.addReferenceBookItem(aspectId, parentId, "value1")
        referenceBookService.addReferenceBookItem(aspectId, parentId, "value1")
    }

    @Test
    fun correctMoveItemsTest() {
        val aspectId = referenceBook.aspectId
        val parentId = referenceBook.id
        val child1 = referenceBookService.addReferenceBookItem(aspectId, parentId, "value1")
        val child2 = referenceBookService.addReferenceBookItem(aspectId, parentId, "value2")
        val child11 = referenceBookService.addReferenceBookItem(aspectId, child1, "value11")
        referenceBookService.moveReferenceBookItem(child11, child2)

        val updatedReferenceBook = referenceBookService.getReferenceBook(aspectId)
        assertTrue("`root.value1` has no child", updatedReferenceBook["value1"]!!.children.isEmpty())
        assertTrue("`root.value2` has 1 child", updatedReferenceBook["value2"]!!.children.size == 1)
    }

    @Test(expected = RefBookItemMoveImpossible::class)
    fun unCorrectMoveItemsTest() {
        val aspectId = referenceBook.aspectId
        val child1 = referenceBookService.addReferenceBookItem(aspectId, referenceBook.id, "value1")
        val child11 = referenceBookService.addReferenceBookItem(aspectId, child1, "value11")
        referenceBookService.moveReferenceBookItem(child1, child11)
    }

    @Test
    fun correctChangeValueTest() {
        val childId = referenceBookService.addReferenceBookItem(referenceBook.aspectId, referenceBook.id, "value1")
        referenceBookService.changeValue(childId, "value2")
        val updated = referenceBookService.getReferenceBookItem(childId)
        assertTrue("Value should be changed", updated.value == "value2")
    }

    @Test(expected = RefBookChildAlreadyExist::class)
    fun unCorrectChangeValueTest() {
        val childId = referenceBookService.addReferenceBookItem(referenceBook.aspectId, referenceBook.id, "value1")
        referenceBookService.addReferenceBookItem(referenceBook.aspectId, referenceBook.id, "value2")
        referenceBookService.changeValue(childId, "value2")
    }

    @Test
    fun removeBookItemTest() {
        val aspectId = referenceBook.aspectId
        val parentId = referenceBook.id
        val child1 = referenceBookService.addReferenceBookItem(aspectId, parentId, "value1")
        val child11 = referenceBookService.addReferenceBookItem(aspectId, child1, "value11")

        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(child1))

        val updatedBook = referenceBookService.getReferenceBook(aspectId)
        assertNull(updatedBook["value1"])
    }

    @Test(expected = RefBookItemConcurrentModificationException::class)
    fun removeBookItemConcurrentRemoveChildTest() {
        val aspectId = referenceBook.aspectId
        val child1 = referenceBookService.addReferenceBookItem(aspectId, referenceBook.id, "value1")
        val child11 = referenceBookService.addReferenceBookItem(aspectId, child1, "value11")
        val child111 = referenceBookService.addReferenceBookItem(aspectId, child11, "value111")

        val forRemoving = referenceBookService.getReferenceBookItem(child1)

        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(child111))
        referenceBookService.removeReferenceBookItem(forRemoving)
    }

    @Test(expected = RefBookItemConcurrentModificationException::class)
    fun removeBookItemConcurrentUpdatingChildTest() {
        val aspectId = referenceBook.aspectId
        val child1 = referenceBookService.addReferenceBookItem(aspectId, referenceBook.id, "value1")
        val child11 = referenceBookService.addReferenceBookItem(aspectId, child1, "value11")

        val forRemoving = referenceBookService.getReferenceBookItem(child1)

        referenceBookService.changeValue(child11, "newValue")
        referenceBookService.removeReferenceBookItem(forRemoving)
    }

    @Test(expected = RefBookItemConcurrentModificationException::class)
    fun removeBookItemConcurrentUpdatingTest() {
        val aspectId = referenceBook.aspectId
        val parentId = referenceBook.id
        val child1 = referenceBookService.addReferenceBookItem(aspectId, parentId, "value1")

        val forRemoving = referenceBookService.getReferenceBookItem(child1)

        referenceBookService.changeValue(child1, "newValue")
        referenceBookService.removeReferenceBookItem(forRemoving)
    }
}