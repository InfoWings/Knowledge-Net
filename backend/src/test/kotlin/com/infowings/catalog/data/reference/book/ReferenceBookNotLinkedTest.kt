package com.infowings.catalog.data.reference.book

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.randomName
import org.junit.Assert
import org.junit.Assert.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
class ReferenceBookNotLinkedTest {

    @Autowired
    private lateinit var referenceBookService: ReferenceBookService
    @Autowired
    private lateinit var aspectService: AspectService

    private lateinit var aspect: AspectData
    private lateinit var referenceBook: ReferenceBook

    private val username = "admin"

    private val refBookName = randomName()

    @BeforeEach
    fun initTestData() {
        aspect = aspectService.save(AspectData("", randomName(), null, null, BaseType.Text.name), username)
        referenceBook = referenceBookService.createReferenceBook(refBookName, aspect.idStrict(), username)
    }

    @Test
    fun saveAlreadyExistBookTest() {
        assertThrows<RefBookAlreadyExist> {
            referenceBookService.createReferenceBook(randomName(), aspect.idStrict(), username)
        }
    }

    @Test
    fun saveReferenceBookTest() {
        assertTrue("Saved reference book name should be equals before saving name", referenceBook.name == refBookName)
    }

    @Test
    fun findReferenceBookTest() {
        assertEquals(referenceBookService.getReferenceBook(aspect.idStrict()), referenceBook)
    }

    @Test
    fun getReferenceBookOrNullTest() {
        val anotherAspect = aspectService.save(AspectData("", randomName(), Metre.name, null, null), username)
        assertEquals(referenceBookService.getReferenceBookOrNull(aspect.idStrict()), referenceBook)
        assertNull(referenceBookService.getReferenceBookOrNull(aspect.id + "1"))
        assertNull(referenceBookService.getReferenceBookOrNull(anotherAspect.idStrict()))
    }

    @Test
    fun findNotExistingReferenceBookTest() {
        assertThrows<RefBookNotExist> { referenceBookService.getReferenceBook(aspect.id + "1") }
    }

    @Test
    fun updateReferenceBookTest() {
        val newName = "newName"
        referenceBookService.updateReferenceBook(referenceBook.copy(name = newName), username)
        val updatedReferenceBook = referenceBookService.getReferenceBook(aspect.idStrict())
        assertEquals(referenceBook.copy(name = newName, version = updatedReferenceBook.version), updatedReferenceBook)
    }

    @Test
    fun updateReferenceBookConcurrentModificationTest() {
        referenceBookService.updateReferenceBook(referenceBook.copy(name = "name1"), username)
        assertThrows<RefBookConcurrentModificationException> {
            referenceBookService.updateReferenceBook(referenceBook.copy(name = "name2"), username)
        }
    }

    @Test
    fun updateNotExistReferenceBookTest() {
        assertThrows<RefBookNotExist> {
            referenceBookService.updateReferenceBook(
                referenceBook.copy(aspectId = aspect.id + "1", name = "newName"),
                username
            )
        }
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

    @Test
    fun addChildrenWithSameValueAsOtherChildrenTest() {
        addReferenceBookItem(referenceBook.id, "value1")
        assertThrows<RefBookChildAlreadyExist> { addReferenceBookItem(referenceBook.id, "value1") }
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

    @Test
    fun unCorrectMoveItemsTest() {
        val child1 = addReferenceBookItem(referenceBook.id, "value1")
        val child11 = addReferenceBookItem(child1, "value11")
        assertThrows<RefBookItemMoveImpossible> {
            referenceBookService.moveReferenceBookItem(
                referenceBookService.getReferenceBookItem(child1),
                referenceBookService.getReferenceBookItem(child11),
                username
            )
        }
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

    @Test
    fun concurrentChangeValueTest() {
        val childId = addReferenceBookItem(referenceBook.id, "value1")
        val version = referenceBookService.getReferenceBookItem(childId).version
        changeValue(childId, "value2", version)
        assertThrows<RefBookConcurrentModificationException> { changeValue(childId, "value3", version) }
    }

    @Test
    fun unCorrectChangeValueTest() {
        val parentId = referenceBook.id
        val childId = addReferenceBookItem(parentId, "value1")
        val childVertex = referenceBookService.getReferenceBookItem(childId)
        addReferenceBookItem(parentId, "value2")
        assertThrows<RefBookChildAlreadyExist> { changeValue(childId, "value2", childVertex.version) }
    }

    @Test
    fun removeBookItemTest() {
        val child1 = addReferenceBookItem(referenceBook.id, "value1")
        addReferenceBookItem(child1, "value11")

        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(child1), username)

        val updatedBook = referenceBookService.getReferenceBook(referenceBook.aspectId)
        assertNull(updatedBook.children.firstOrNull { it.value == "value1" })
    }

    @Test
    fun removeBookItemConcurrentRemoveChildTest() {
        val child1 = addReferenceBookItem(referenceBook.id, "value1")
        val child11 = addReferenceBookItem(child1, "value11")
        val child111 = addReferenceBookItem(child11, "value111")

        val forRemoving = referenceBookService.getReferenceBookItem(child1)

        referenceBookService.removeReferenceBookItem(referenceBookService.getReferenceBookItem(child111), username)
        assertThrows<RefBookConcurrentModificationException> {
            referenceBookService.removeReferenceBookItem(forRemoving, username)
        }
    }

    @Test
    fun removeBookItemConcurrentUpdatingChildTest() {
        val child1 = addReferenceBookItem(referenceBook.id, "value1")
        val child11 = addReferenceBookItem(child1, "value11")

        val forRemoving = referenceBookService.getReferenceBookItem(child1)

        assertThrows<RefBookConcurrentModificationException> {
            changeValue(child11, "newValue")
            referenceBookService.removeReferenceBookItem(forRemoving, username)
        }
    }

    @Test
    fun removeBookItemConcurrentUpdatingTest() {
        val child1 = addReferenceBookItem(referenceBook.id, "value1")

        val forRemoving = referenceBookService.getReferenceBookItem(child1)

        assertThrows<RefBookConcurrentModificationException> {
            changeValue(child1, "newValue")
            referenceBookService.removeReferenceBookItem(forRemoving, username)
        }
    }

    @Test
    fun removeBookConcurrentNameUpdating() {
        addReferenceBookItem(referenceBook.id, "some")
        val book = referenceBookService.getReferenceBook(referenceBook.aspectId)
        referenceBookService.updateReferenceBook(book.copy(name = "newName"), username)
        assertThrows<RefBookConcurrentModificationException> {
            referenceBookService.removeReferenceBook(book, username)
        }
    }

    @Test
    fun removeBookConcurrentAddingItem() {
        addReferenceBookItem(referenceBook.id, "some")
        val book = referenceBookService.getReferenceBook(referenceBook.aspectId)
        addReferenceBookItem(book.id, "another")
        assertThrows<RefBookConcurrentModificationException> {
            referenceBookService.removeReferenceBook(book, username)
        }
    }

    @Test
    fun removeItemConcurrentAddingItem() {
        val itemId = addReferenceBookItem(referenceBook.id, "some")
        val bookItem = referenceBookService.getReferenceBookItem(itemId)
        addReferenceBookItem(itemId, "another")
        assertThrows<RefBookConcurrentModificationException> {
            referenceBookService.removeReferenceBookItem(bookItem, username)
        }
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

    @Test
    fun testCreateRefBookSameNameSpaces() {
        assertThrows<RefBookAlreadyExist> {
            referenceBookService.createReferenceBook(referenceBook.name + " ", aspect.id!!, username)
        }
    }

    @Test
    fun testChangeRefBookSameNameSpaces() {
        assertThrows<RefBookAlreadyExist> {
            val referenceBook2 = referenceBookService.createReferenceBook(randomName(), aspect.id!!, username)
            referenceBookService.updateReferenceBook(referenceBook2.copy(name = " ${referenceBook2.name}   "), username)
        }
    }

    @Test
    fun testCreateRefBookItemSameNameSpaces() {
        addReferenceBookItem(referenceBook.id, "value")
        assertThrows<RefBookChildAlreadyExist> { addReferenceBookItem(referenceBook.id, "value ") }
    }

    @Test
    fun testUpdateRefBookItemSameNameSpaces() {
        addReferenceBookItem(referenceBook.id, "value ")
        val resId = addReferenceBookItem(referenceBook.id, "value2")
        val res = referenceBookService.getReferenceBookItem(resId)
        assertThrows<RefBookChildAlreadyExist> {
            referenceBookService.updateReferenceBookItem(res.copy(value = "value   "), username)
        }
    }

    @Test
    fun saveWithSpacesAround() {
        val aspect2 = aspectService.save(AspectData(name = "testAspect", baseType = BaseType.Text.name), username)
        var refBook = referenceBookService.createReferenceBook("  name  ", aspectId = aspect2.id!!, username = username)
        assertEquals("Reference book should have trimmed name", "name", refBook.name)

        referenceBookService.updateReferenceBook(refBook.copy(description = "   description   "), username)
        refBook = referenceBookService.getReferenceBook(refBook.aspectId)
        assertEquals("Reference book should have trimmed description", "description", refBook.description)

        val itemId = referenceBookService.addReferenceBookItem(
            refBook.id,
            ReferenceBookItem(id = "", version = 0, value = " val  ", description = " d   ", deleted = false, children = emptyList(), guid = null),
            username
        )

        var bookItem = referenceBookService.getReferenceBookItem(itemId)

        assertEquals("Reference book item should have trimmed value", "val", bookItem.value)
        assertEquals("Reference book should have trimmed description", "d", bookItem.description)

        refBook = referenceBookService.getReferenceBook(refBook.aspectId)
        referenceBookService.updateReferenceBook(refBook.copy(name = "  newName ", description = " newDesc"), username)
        refBook = referenceBookService.getReferenceBook(refBook.aspectId)
        assertEquals("Reference book should have trimmed name", "newName", refBook.name)
        assertEquals("Reference book should have trimmed description", "newDesc", refBook.description)

        referenceBookService.updateReferenceBookItem(bookItem.copy(value = " newVal  ", description = " newD "), username)
        bookItem = referenceBookService.getReferenceBookItem(itemId)
        assertEquals("Reference book item should have trimmed value", "newVal", bookItem.value)
        assertEquals("Reference book should have trimmed description", "newD", bookItem.description)
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
            0,
            null
        )
    }

    private fun changeValue(id: String, value: String, version: Int = 0) = referenceBookService.updateReferenceBookItem(
        ReferenceBookItem(
            id,
            value,
            null,
            emptyList(),
            false,
            version,
            null
        ),
        username
    )
}