package com.infowings.catalog.data.reference.book

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.randomName
import junit.framework.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReferenceBookNotLinkedCleanTest {

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
    fun getAllReferenceBooksTest() {
        val anotherAspect = aspectService.save(AspectData("", "anotherAspect", null, null, BaseType.Text.name), username)
        val anotherBook = referenceBookService.createReferenceBook(refBookName, anotherAspect.idStrict(), username)
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
    fun removeBookTest() {
        val anotherAspect = aspectService.save(AspectData("", randomName(), null, null, BaseType.Text.name), username)
        val anotherAspectId = anotherAspect.idStrict()
        var bookForRemoving = referenceBookService.createReferenceBook("forRemovingBook", anotherAspectId, username)
        addReferenceBookItem(bookForRemoving.id, "itemValue")
        bookForRemoving = referenceBookService.getReferenceBook(anotherAspectId)
        referenceBookService.removeReferenceBook(bookForRemoving, username)
        assertEquals(listOf(referenceBook), referenceBookService.getAllReferenceBooks())
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

}