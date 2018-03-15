package com.infowings.catalog.external

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.data.ReferenceBookService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/book")
class ReferenceBookApi(val referenceBookService: ReferenceBookService) {

    @GetMapping("all")
    fun all(): Set<ReferenceBook> {
        return referenceBookService.getReferenceBooks()
    }

    @GetMapping("get/{name}")
    fun getByName(@PathVariable("name") name: String): ReferenceBook {
        return referenceBookService.getReferenceBook(name)
    }

    @PostMapping("create")
    fun create(@RequestBody referenceBook: ReferenceBook): ReferenceBook {
        return referenceBookService.createReferenceBook(referenceBook.name, referenceBook.aspectId)
    }

    /*
    @PostMapping("update")
    fun update(@RequestBody referenceBook: ReferenceBook): ReferenceBook {
        return referenceBookService.update(name, aspectId)
    }*/
}