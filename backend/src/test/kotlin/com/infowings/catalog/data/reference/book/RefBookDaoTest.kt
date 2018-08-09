package com.infowings.catalog.data.reference.book

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.id
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [MasterCatalog::class])
class RefBookDaoTest {
    private val username = "admin"

    @Autowired
    lateinit var refBookDao: ReferenceBookDao

    @Autowired
    lateinit var refBookService: ReferenceBookService

    @Autowired
    lateinit var aspectService: AspectService

    @Test
    fun testRefBookFindOne() {
        val aspectName = "testRefBookFindOne-aspect"
        val aspectDescr = "aspect description"
        val created = aspectService.save(
            AspectData(
                id = "", name = aspectName, description = aspectDescr,
                version = 0, deleted = false, baseType = BaseType.Text.name
            ), username
        )
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
        val aspectName = randomName()
        val aspectDescr = "aspect description"
        val created = aspectService.save(
            AspectData(
                id = "", name = aspectName, description = aspectDescr,
                version = 0, deleted = false, baseType = BaseType.Text.name
            ), username
        )
        val aspectId = created.id ?: throw IllegalStateException("aspect id is null")

        val rbName = "rb"
        val refBook = refBookService.createReferenceBook(rbName, aspectId, username)

        val refBookVertices = refBookDao.findStr(listOf(aspectId))

        assertEquals(0, refBookVertices.size)
    }
}