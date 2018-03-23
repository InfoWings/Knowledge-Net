package com.infowings.catalog.external

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.common.ReferenceBookItemData
import com.infowings.catalog.common.ReferenceBooksList
import com.infowings.catalog.data.ReferenceBookService
import com.infowings.catalog.loggerFor
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder

@RestController
@RequestMapping("api/book")
class ReferenceBookApi(val referenceBookService: ReferenceBookService) {

    @GetMapping("all")
    fun getAll(): ReferenceBooksList {
        logger.debug("Getting all reference books")
        return ReferenceBooksList(referenceBookService.getAllReferenceBooks())
    }

    @GetMapping("get")
    fun getByAspectId(@RequestParam(value = "aspectId", required = true) encodedAspectId: String): ReferenceBook {
        val aspectId = URLDecoder.decode(encodedAspectId, "UTF-8")
        logger.debug("Getting reference books by aspectId=$aspectId")
        return referenceBookService.getReferenceBook(aspectId)
    }

    @PostMapping("create")
    fun create(@RequestBody book: ReferenceBookData): ReferenceBook {
        logger.debug("Creating reference book with name=${book.name} for aspectId=${book.aspectId}")
        return referenceBookService.createReferenceBook(book.name, book.aspectId)
    }

    @PostMapping("update")
    fun update(@RequestBody book: ReferenceBookData): ReferenceBook {
        val aspectId = book.aspectId
        val newName: String = book.name
        logger.debug("Updating reference book name to $newName where aspectId=$aspectId")
        return referenceBookService.updateReferenceBook(aspectId, newName)
    }

    @PostMapping("item/create")
    fun createItem(@RequestBody bookItemData: ReferenceBookItemData): ReferenceBook {
        logger.debug("Creating reference book item")
        return referenceBookService.addItemAndGetReferenceBook(
            bookItemData.aspectId,
            bookItemData.parentId,
            bookItemData.value
        )
    }

    @PostMapping("item/update")
    fun updateItem(@RequestBody bookItemData: ReferenceBookItemData): ReferenceBook {
        logger.debug("Updating reference book item with id=${bookItemData.id}")
        return referenceBookService.updateItemAndGetReferenceBook(
            bookItemData.aspectId,
            bookItemData.id!!,
            bookItemData.value
        )
    }

    private val logger = loggerFor<ReferenceBookApi>()
}