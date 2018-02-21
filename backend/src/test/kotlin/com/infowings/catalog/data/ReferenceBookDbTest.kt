package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.ReferenceBook
import org.junit.Assert.*
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
    lateinit var referenceBookService: ReferenceBookService

    val referenceBook = ReferenceBook(name = "tool", description = "Simple dictionary for tools")

    @Test
    fun saveReferenceBookTest() {
        val res = referenceBookService.saveReferenceBook(referenceBook)
        assertTrue("Saved reference book must contains generated id", res.id != null)
    }

    @Test
    fun findReferenceBookTest() {
        val saved = referenceBookService.saveReferenceBook(referenceBook)
        val found = referenceBookService.getReferenceBook(referenceBook.name)
        assertTrue("Found reference book must be equals with saved", found!! == saved)
        assertNull("Found reference book should be null", referenceBookService.getReferenceBook("random"))
    }

    @Test
    fun removeReferenceBookTest() {
        referenceBookService.saveReferenceBook(referenceBook)
        referenceBookService.removeReferenceBook(referenceBook.name)
        assertNull("Database must not contain deleted by name reference book", referenceBookService.getReferenceBook(referenceBook.name))
    }

    @Test
    fun changeReferenceBookDescriptionTest() {
        referenceBookService.saveReferenceBook(referenceBook)
        referenceBookService.changeReferenceBookDescription(referenceBook.name, "New Description")
        assertEquals("Reference book from db should have new description",
                referenceBookService.getReferenceBook(referenceBook.name)!!.description,
                "New Description")
    }
}