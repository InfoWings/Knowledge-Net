package com.infowings.catalog.data.reference.book

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.Aspect
import com.infowings.catalog.data.aspect.AspectService
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
class ReferenceBookServiceTest {

    @Autowired
    private lateinit var referenceBookService: ReferenceBookService
    @Autowired
    private lateinit var aspectService: AspectService

    private lateinit var aspect: Aspect
    private lateinit var referenceBook: ReferenceBook

    private val username = "admin"

    @Before
    fun initTestData() {
        aspect = aspectService.save(AspectData("", "aspect", null, null, BaseType.Text.name), username)
        referenceBook = referenceBookService.createReferenceBook("Example", aspect.id, username)
    }

    @Test(expected = RefBookAlreadyExist::class)
    fun saveAlreadyExistBookTest() {
        referenceBookService.createReferenceBook("some", aspect.id, username)
    }

    @Test
    fun saveReferenceBookTest() {
        assertTrue("Saved reference book name should be equals before saving name", referenceBook.name == "Example")
    }

    @Test
    fun getAllReferenceBooksTest() {
        val anotherAspect =
            aspectService.save(AspectData("", "anotherAspect", null, null, BaseType.Text.name), username)
        val anotherBook = referenceBookService.createReferenceBook("Example2", anotherAspect.id, username)
        val thirdAspect = aspectService.save(AspectData("", "third", null, null, BaseType.Text.name), username)
        val forDeletingBook = referenceBookService.createReferenceBook("forDeleting", thirdAspect.id, username)
        referenceBookService.removeReferenceBook(forDeletingBook, username, force = true)
        assertEquals(referenceBookService.getAllReferenceBooks().toSet(), setOf(anotherBook, referenceBook))
    }

    @Test
    fun findReferenceBookTest() {
        assertEquals(referenceBookService.getReferenceBook(aspect.id), referenceBook)
    }

    @Test
    fun getReferenceBookOrNullTest() {
        val anotherAspect = aspectService.save(AspectData("", "anotherAspect", Metre.name, null, null), username)
        assertEquals(referenceBookService.getReferenceBookOrNull(aspect.id), referenceBook)
        assertNull(referenceBookService.getReferenceBookOrNull(aspect.id + "1"))
        assertNull(referenceBookService.getReferenceBookOrNull(anotherAspect.id))
    }

    @Test(expected = RefBookNotExist::class)
    fun findNotExistingReferenceBookTest() {
        referenceBookService.getReferenceBook(aspect.id + "1")
    }

    @Test
    fun updateReferenceBookTest() {
        val newName = "newName"
        referenceBookService.updateReferenceBook(referenceBook.copy(name = newName), username)
        val updatedReferenceBook = referenceBookService.getReferenceBook(aspect.id)
        assertEquals(referenceBook.copy(name = newName, version = updatedReferenceBook.version), updatedReferenceBook)
    }

    @Test(expected = RefBookConcurrentModificationException::class)
    fun updateReferenceBookConcurrentModificationTest() {
        referenceBookService.updateReferenceBook(referenceBook.copy(name = "name1"), username)
        referenceBookService.updateReferenceBook(referenceBook.copy(name = "name2"), username)
    }

    @Test(expected = RefBookNotExist::class)
    fun updateNotExistReferenceBookTest() {
        referenceBookService.updateReferenceBook(
            referenceBook.copy(aspectId = aspect.id + "1", name = "newName"),
            username
        )
    }

    @Test
    fun addReferenceBookItemAsAChildToExistingItemTest() {
        val newId = addReferenceBookItem(referenceBook.aspectId, referenceBook.aspectId, "value")
        assertTrue("New item was created", referenceBookService.getReferenceBookItem(newId).value == "value")
    }

    @Test
    fun addChildrenTest() {
        val aspectId = referenceBook.aspectId
        val id1 = addReferenceBookItem(aspectId, aspectId, "value1")
        addReferenceBookItem(aspectId, aspectId, "value2")
        val id11 = addReferenceBookItem(aspectId, id1, "value11")
        addReferenceBookItem(aspectId, id11, "value111")

        val updatedReferenceBook = referenceBookService.getReferenceBook(aspectId)
        assertTrue("Reference book has 2 children", updatedReferenceBook.children.size == 2)
        val child1 = updatedReferenceBook.children.first { it.value == "value1" }
        assertTrue("RefBook.`value1` has 1 child", child1.children.size == 1)
        assertTrue(
            "`RefBook.value1.value11` has 1 child",
            child1.children.first { it.value == "value11" }.children.size == 1
        )
    }

    @Test(expected = RefBookChildAlreadyExist::class)
    fun addChildrenWithSameValueAsOtherChildrenTest() {
        val aspectId = referenceBook.aspectId
        val parentId = referenceBook.aspectId
        addReferenceBookItem(aspectId, parentId, "value1")
        addReferenceBookItem(aspectId, parentId, "value1")
    }

    @Test
    fun correctMoveItemsTest() {
        val aspectId = referenceBook.aspectId
        val parentId = referenceBook.aspectId
        val child1 = addReferenceBookItem(aspectId, parentId, "value1")
        val child2 = addReferenceBookItem(aspectId, parentId, "value2")
        val child11 = addReferenceBookItem(aspectId, child1, "value11")
        referenceBookService.moveReferenceBookItem(
            referenceBookService.getReferenceBookItem(child11),
            referenceBookService.getReferenceBookItem(child2),
            username
        )

        val updatedReferenceBook = referenceBookService.getReferenceBook(aspectId)
        assertTrue(
            "`RefBook.value1` has no child",
            updatedReferenceBook.children.first { it.value == "value1" }.children.isEmpty()
        )
        assertTrue(
            "`RefBook.value2` has 1 child",
            updatedReferenceBook.children.first { it.value == "value2" }.children.size == 1
        )
    }

    @Test(expected = RefBookItemMoveImpossible::class)
    fun unCorrectMoveItemsTest() {
        val aspectId = referenceBook.aspectId
        val child1 = addReferenceBookItem(aspectId, aspectId, "value1")
        val child11 = addReferenceBookItem(aspectId, child1, "value11")
        referenceBookService.moveReferenceBookItem(
            referenceBookService.getReferenceBookItem(child1),
            referenceBookService.getReferenceBookItem(child11),
            username
        )
    }

    @Test
    fun correctChangeValueWhenParentIsRefBookTest() {
        val aspectId = referenceBook.aspectId
        val childId = addReferenceBookItem(aspectId, aspectId, "value1")
        val childVertex = referenceBookService.getReferenceBookItem(childId)
        changeValue(childId, "value2", childVertex.version)
        val updated = referenceBookService.getReferenceBookItem(childId)
        assertTrue("Value should be changed", updated.value == "value2")
    }

    @Test
    fun correctChangeValueWhenParentIsItemTest() {
        val aspectId = referenceBook.aspectId
        val childId1 = addReferenceBookItem(aspectId, aspectId, "value1")
        val childId11 = addReferenceBookItem(aspectId, childId1, "value11")
        val childVertex = referenceBookService.getReferenceBookItem(childId11)
        changeValue(childId11, "value12", childVertex.version)
        val updated = referenceBookService.getReferenceBookItem(childId11)
        assertTrue("Value should be changed", updated.value == "value12")
    }

    @Test(expected = RefBookItemConcurrentModificationException::class)
    fun concurrentChangeValueTest() {
        val aspectId = referenceBook.aspectId
        val childId = addReferenceBookItem(aspectId, aspectId, "value1")
        val version = referenceBookService.getReferenceBookItem(childId).version
        changeValue(childId, "value2", version)
        changeValue(childId, "value3", version)
    }

    @Test(expected = RefBookChildAlreadyExist::class)
    fun unCorrectChangeValueTest() {
        val aspectId = referenceBook.aspectId
        val childId = addReferenceBookItem(aspectId, aspectId, "value1")
        val childVertex = referenceBookService.getReferenceBookItem(childId)
        addReferenceBookItem(aspectId, aspectId, "value2")
        changeValue(childId, "value2", childVertex.version)
    }

    @Test
    fun removeBookItemTest() {
        val aspectId = referenceBook.aspectId
        val child1 = addReferenceBookItem(aspectId, aspectId, "value1")
        addReferenceBookItem(aspectId, child1, "value11")

        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(child1), username)

        val updatedBook = referenceBookService.getReferenceBook(aspectId)
        assertNull(updatedBook.children.firstOrNull { it.value == "value1" })
    }

    @Test(expected = RefBookItemConcurrentModificationException::class)
    fun removeBookItemConcurrentRemoveChildTest() {
        val aspectId = referenceBook.aspectId
        val child1 = addReferenceBookItem(aspectId, aspectId, "value1")
        val child11 = addReferenceBookItem(aspectId, child1, "value11")
        val child111 = addReferenceBookItem(aspectId, child11, "value111")

        val forRemoving = referenceBookService.getReferenceBookItem(child1)

        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(child111), username)
        referenceBookService.removeReferenceBookItem(forRemoving, username)
    }

    @Test(expected = RefBookItemConcurrentModificationException::class)
    fun removeBookItemConcurrentUpdatingChildTest() {
        val aspectId = referenceBook.aspectId
        val child1 = addReferenceBookItem(aspectId, aspectId, "value1")
        val child11 = addReferenceBookItem(aspectId, child1, "value11")

        val forRemoving = referenceBookService.getReferenceBookItem(child1)

        changeValue(child11, "newValue")
        referenceBookService.removeReferenceBookItem(forRemoving, username)
    }

    @Test(expected = RefBookItemConcurrentModificationException::class)
    fun removeBookItemConcurrentUpdatingTest() {
        val aspectId = referenceBook.aspectId
        val child1 = addReferenceBookItem(aspectId, aspectId, "value1")

        val forRemoving = referenceBookService.getReferenceBookItem(child1)

        changeValue(child1, "newValue")
        referenceBookService.removeReferenceBookItem(forRemoving, username)
    }

    @Test
    fun removeBookTest() {
        val anotherAspect =
            aspectService.save(AspectData("", "anotherAspect", null, null, BaseType.Text.name), username)
        val anotherAspectId = anotherAspect.id
        var bookForRemoving = referenceBookService.createReferenceBook("forRemovingBook", anotherAspectId, username)
        referenceBookService.addReferenceBookItem(
            createReferenceBookItem(anotherAspectId, bookForRemoving.aspectId, "itemValue"), username
        )
        bookForRemoving = referenceBookService.getReferenceBook(anotherAspectId)
        referenceBookService.removeReferenceBook(bookForRemoving, username)
        assertEquals(listOf(referenceBook), referenceBookService.getAllReferenceBooks())
    }

    @Test(expected = RefBookConcurrentModificationException::class)
    fun removeBookConcurrentNameUpdating() {
        val aspectId = referenceBook.aspectId
        referenceBookService.addReferenceBookItem(createReferenceBookItem(aspectId, aspectId, "some"), username)
        val book = referenceBookService.getReferenceBook(aspectId)
        referenceBookService.updateReferenceBook(book.copy(name = "newName"), username)
        referenceBookService.removeReferenceBook(book, username)
    }

    @Test(expected = RefBookConcurrentModificationException::class)
    fun removeBookConcurrentAddingItem() {
        val aspectId = referenceBook.aspectId
        referenceBookService.addReferenceBookItem(createReferenceBookItem(aspectId, aspectId, "some"), username)
        val book = referenceBookService.getReferenceBook(aspectId)
        addReferenceBookItem(aspectId, book.aspectId, "another")
        referenceBookService.removeReferenceBook(book, username)
    }

    @Test(expected = RefBookItemConcurrentModificationException::class)
    fun removeItemConcurrentAddingItem() {
        val aspectId = referenceBook.aspectId
        val itemId =
            referenceBookService.addReferenceBookItem(createReferenceBookItem(aspectId, aspectId, "some"), username)
        val bookItem = referenceBookService.getReferenceBookItem(itemId)
        addReferenceBookItem(aspectId, itemId, "another")
        referenceBookService.removeReferenceBookItem(bookItem, username)
    }

    private fun addReferenceBookItem(aspectId: String, parentId: String, value: String): String =
        referenceBookService.addReferenceBookItem(createReferenceBookItem(aspectId, parentId, value), username)

    private fun createReferenceBookItem(
        aspectId: String,
        parentId: String,
        value: String
    ): ReferenceBookItem {
        return ReferenceBookItem(
            parentId,
            "",
            value,
            emptyList(),
            false,
            0
        )
    }

    private fun changeValue(id: String, value: String, version: Int = 0) = referenceBookService.changeValue(
        ReferenceBookItem(
            "",
            id,
            value,
            emptyList(),
            false,
            version
        ),
        username
    )
}