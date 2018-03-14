package com.infowings.catalog.external

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.data.ReferenceBookService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/book")
class ReferenceBookApi(val referenceBookService: ReferenceBookService) {


    @GetMapping("all")
    fun getReferenceBooks(): Set<ReferenceBook> {
        return referenceBookService.getReferenceBooks()
    }

    @GetMapping("get/{name}")
    fun getReferenceBooks(@PathVariable("name") name: String): ReferenceBook {
        return referenceBookService.getReferenceBook(name)
    }

    @PostMapping("create")
    fun getReferenceBooks(@RequestParam("name") name: String, @RequestParam("aspectId") aspectId: String): ReferenceBook {
        return referenceBookService.createReferenceBook(name, aspectId)
    }
}