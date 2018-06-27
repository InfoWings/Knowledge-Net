package com.infowings.catalog.data.reference.book

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectService
import org.junit.Assert
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
class ReferenceBookNotLinkedTest {

    @Autowired
    private lateinit var referenceBookService: ReferenceBookService
    @Autowired
    private lateinit var aspectService: AspectService

    private lateinit var aspect: AspectData
    private lateinit var referenceBook: ReferenceBook

    private val username = "admin"

    @Before
    fun initTestData() {
        aspect = aspectService.save(AspectData("", "aspect", null, null, BaseType.Text.name), username)
        referenceBook = referenceBookService.createReferenceBook("Example", aspect.idStrict(), username)
    }

    @Test(expected = RefBookAlreadyExist::class)
    fun saveAlreadyExistBookTest() {
        referenceBookService.createReferenceBook("some", aspect.idStrict(), username)
    }

    @Test
    fun saveReferenceBookTest() {
        assertTrue("Saved reference book name should be equals before saving name", referenceBook.name == "Example")
    }

    @Test
    fun getAllReferenceBooksTest() {
        val anotherAspect =
            aspectService.save(AspectData("", "anotherAspect", null, null, BaseType.Text.name), username)
        val anotherBook = referenceBookService.createReferenceBook("Example2", anotherAspect.idStrict(), username)
        val itemId = referenceBookService.addReferenceBookItem(anotherBook.id, createReferenceBookItem("v1"), username)
        val anotherBookChild = referenceBookService.getReferenceBookItem(itemId)
        val thirdAspect = aspectService.save(AspectData("", "third", null, null, BaseType.Text.name), username)
        val forDeletingBook = referenceBookService.createReferenceBook("forDeleting", thirdAspect.idStrict(), username)
        referenceBookService.removeReferenceBook(forDeletingBook, username, force = true)
        assertEquals(
            setOf(
                anotherBook.copy(children = listOf(anotherBookChild), version = anotherBook.version.inc()),
                referenceBook
            ),
            referenceBookService.getAllReferenceBooks().toSet()
        )
    }

    @Test
    fun findReferenceBookTest() {
        assertEquals(referenceBookService.getReferenceBook(aspect.idStrict()), referenceBook)
    }

    @Test
    fun getReferenceBookOrNullTest() {
        val anotherAspect = aspectService.save(AspectData("", "anotherAspect", Metre.name, null, null), username)
        assertEquals(referenceBookService.getReferenceBookOrNull(aspect.idStrict()), referenceBook)
        assertNull(referenceBookService.getReferenceBookOrNull(aspect.id + "1"))
        assertNull(referenceBookService.getReferenceBookOrNull(anotherAspect.idStrict()))
    }

    @Test(expected = RefBookNotExist::class)
    fun findNotExistingReferenceBookTest() {
        referenceBookService.getReferenceBook(aspect.id + "1")
    }

    @Test
    fun updateReferenceBookTest() {
        val newName = "newName"
        referenceBookService.updateReferenceBook(referenceBook.copy(name = newName), username)
        val updatedReferenceBook = referenceBookService.getReferenceBook(aspect.idStrict())
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
        val newId = addReferenceBookItem(referenceBook.id, "value")
        assertTrue("New item was created", referenceBookService.getReferenceBookItem(newId).value == "value")
    }

    @Test
    fun addChildrenTest() {
        val id1 = addReferenceBookItem(referenceBook.id, "value1")
        addReferenceBookItem(referenceBook.id, "value2")
        val id11 = addReferenceBookItem(id1, "value11")
        addReferenceBookItem(id11, "value111")

        val updatedReferenceBook = referenceBookService.getReferenceBook(referenceBook.aspectId)
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
        addReferenceBookItem(referenceBook.id, "value1")
        addReferenceBookItem(referenceBook.id, "value1")
    }

    @Test
    fun correctMoveItemsTest() {
        val parentId = referenceBook.id
        val child1 = addReferenceBookItem(parentId, "value1")
        val child2 = addReferenceBookItem(parentId, "value2")
        val child11 = addReferenceBookItem(child1, "value11")
        referenceBookService.moveReferenceBookItem(
            referenceBookService.getReferenceBookItem(child11),
            referenceBookService.getReferenceBookItem(child2),
            username
        )

        val updatedReferenceBook = referenceBookService.getReferenceBook(referenceBook.aspectId)
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
        val child1 = addReferenceBookItem(referenceBook.id, "value1")
        val child11 = addReferenceBookItem(child1, "value11")
        referenceBookService.moveReferenceBookItem(
            referenceBookService.getReferenceBookItem(child1),
            referenceBookService.getReferenceBookItem(child11),
            username
        )
    }

    @Test
    fun correctChangeValueWhenParentIsRefBookTest() {
        val childId = addReferenceBookItem(referenceBook.id, "value1")
        val childVertex = referenceBookService.getReferenceBookItem(childId)
        changeValue(childId, "value2", childVertex.version)
        val updated = referenceBookService.getReferenceBookItem(childId)
        assertTrue("Value should be changed", updated.value == "value2")
    }

    @Test
    fun correctChangeValueWhenParentIsItemTest() {
        val childId1 = addReferenceBookItem(referenceBook.id, "value1")
        val childId11 = addReferenceBookItem(childId1, "value11")
        val childVertex = referenceBookService.getReferenceBookItem(childId11)
        changeValue(childId11, "value12", childVertex.version)
        val updated = referenceBookService.getReferenceBookItem(childId11)
        assertTrue("Value should be changed", updated.value == "value12")
    }

    @Test(expected = RefBookConcurrentModificationException::class)
    fun concurrentChangeValueTest() {
        val childId = addReferenceBookItem(referenceBook.id, "value1")
        val version = referenceBookService.getReferenceBookItem(childId).version
        changeValue(childId, "value2", version)
        changeValue(childId, "value3", version)
    }

    @Test(expected = RefBookChildAlreadyExist::class)
    fun unCorrectChangeValueTest() {
        val parentId = referenceBook.id
        val childId = addReferenceBookItem(parentId, "value1")
        val childVertex = referenceBookService.getReferenceBookItem(childId)
        addReferenceBookItem(parentId, "value2")
        changeValue(childId, "value2", childVertex.version)
    }

    @Test
    fun removeBookItemTest() {
        val child1 = addReferenceBookItem(referenceBook.id, "value1")
        addReferenceBookItem(child1, "value11")

        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(child1), username)

        val updatedBook = referenceBookService.getReferenceBook(referenceBook.aspectId)
        assertNull(updatedBook.children.firstOrNull { it.value == "value1" })
    }

    @Test(expected = RefBookConcurrentModificationException::class)
    fun removeBookItemConcurrentRemoveChildTest() {
        val child1 = addReferenceBookItem(referenceBook.id, "value1")
        val child11 = addReferenceBookItem(child1, "value11")
        val child111 = addReferenceBookItem(child11, "value111")

        val forRemoving = referenceBookService.getReferenceBookItem(child1)

        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(child111), username)
        referenceBookService.removeReferenceBookItem(forRemoving, username)
    }

    @Test(expected = RefBookConcurrentModificationException::class)
    fun removeBookItemConcurrentUpdatingChildTest() {
        val child1 = addReferenceBookItem(referenceBook.id, "value1")
        val child11 = addReferenceBookItem(child1, "value11")

        val forRemoving = referenceBookService.getReferenceBookItem(child1)

        changeValue(child11, "newValue")
        referenceBookService.removeReferenceBookItem(forRemoving, username)
    }

    @Test(expected = RefBookConcurrentModificationException::class)
    fun removeBookItemConcurrentUpdatingTest() {
        val child1 = addReferenceBookItem(referenceBook.id, "value1")

        val forRemoving = referenceBookService.getReferenceBookItem(child1)

        changeValue(child1, "newValue")
        referenceBookService.removeReferenceBookItem(forRemoving, username)
    }

    @Test
    fun removeBookTest() {
        val anotherAspect =
            aspectService.save(AspectData("", "anotherAspect", null, null, BaseType.Text.name), username)
        val anotherAspectId = anotherAspect.idStrict()
        var bookForRemoving = referenceBookService.createReferenceBook("forRemovingBook", anotherAspectId, username)
        addReferenceBookItem(bookForRemoving.id, "itemValue")
        bookForRemoving = referenceBookService.getReferenceBook(anotherAspectId)
        referenceBookService.removeReferenceBook(bookForRemoving, username)
        assertEquals(listOf(referenceBook), referenceBookService.getAllReferenceBooks())
    }

    @Test(expected = RefBookConcurrentModificationException::class)
    fun removeBookConcurrentNameUpdating() {
        addReferenceBookItem(referenceBook.id, "some")
        val book = referenceBookService.getReferenceBook(referenceBook.aspectId)
        referenceBookService.updateReferenceBook(book.copy(name = "newName"), username)
        referenceBookService.removeReferenceBook(book, username)
    }

    @Test(expected = RefBookConcurrentModificationException::class)
    fun removeBookConcurrentAddingItem() {
        addReferenceBookItem(referenceBook.id, "some")
        val book = referenceBookService.getReferenceBook(referenceBook.aspectId)
        addReferenceBookItem(book.id, "another")
        referenceBookService.removeReferenceBook(book, username)
    }

    @Test(expected = RefBookConcurrentModificationException::class)
    fun removeItemConcurrentAddingItem() {
        val itemId = addReferenceBookItem(referenceBook.id, "some")
        val bookItem = referenceBookService.getReferenceBookItem(itemId)
        addReferenceBookItem(itemId, "another")
        referenceBookService.removeReferenceBookItem(bookItem, username)
    }

    @Test
    fun testUpdateSameData() {
        try {
            referenceBookService.updateReferenceBook(referenceBook, username)
        } catch (e: RefBookEmptyChangeException) {
        }
        val updated = referenceBookService.getReferenceBook(referenceBook.aspectId)
        Assert.assertEquals("Same data shouldn't be rewritten", referenceBook.version, updated.version)

        val parentId = referenceBook.id
        val childId = addReferenceBookItem(parentId, "value1")
        val child = referenceBookService.getReferenceBookItem(childId)
        try {
            referenceBookService.updateReferenceBookItem(child, username)
        } catch (e: RefBookEmptyChangeException) {
        }
        val savedChild = referenceBookService.getReferenceBookItem(childId)

        Assert.assertEquals("Same data shouldn't be rewritten", child.version, savedChild.version)
    }

    private fun addReferenceBookItem(parentId: String, value: String): String =
        referenceBookService.addReferenceBookItem(parentId, createReferenceBookItem(value), username)

    private fun createReferenceBookItem(value: String): ReferenceBookItem {
        return ReferenceBookItem(
            "",
            value,
            null,
            emptyList(),
            false,
            0
        )
    }

    private fun changeValue(id: String, value: String, version: Int = 0) = referenceBookService.updateReferenceBookItem(
        ReferenceBookItem(
            id,
            value,
            null,
            emptyList(),
            false,
            version
        ),
        username
    )
}