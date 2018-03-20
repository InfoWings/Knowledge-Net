package com.infowings.catalog.external

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
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
        return ReferenceBooksList(referenceBookService.getReferenceBooks())
    }

    @GetMapping("get")
    fun getByAspectId(@RequestParam(value = "aspectId", required = true) encodedAspectId: String): ReferenceBooksList {
        val aspectId = URLDecoder.decode(encodedAspectId, "UTF-8")
        logger.debug("Getting reference books by aspectId=$aspectId")
        return ReferenceBooksList(referenceBookService.getReferenceBooksByAspectId(aspectId))
    }

    @GetMapping("get/{name}")
    fun getByName(@PathVariable("name") name: String): ReferenceBook {
        logger.debug("Getting reference book by name=$name")
        return referenceBookService.getReferenceBook(name)
    }

    @PostMapping("create")
    fun create(@RequestBody book: ReferenceBookData): ReferenceBook {
        logger.debug("Creating reference book with name=${book.name} for aspectId=${book.aspectId}")
        return referenceBookService.createReferenceBook(book.name, book.aspectId)
    }

    private val logger = loggerFor<ReferenceBookApi>()
}