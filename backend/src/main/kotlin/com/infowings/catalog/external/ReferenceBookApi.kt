package com.infowings.catalog.external

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.ReferenceBooksList
import com.infowings.catalog.data.reference.book.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder

@RestController
@RequestMapping("api/book")
class ReferenceBookApi(val referenceBookService: ReferenceBookService) {

    @GetMapping("all")
    fun getAll(): ReferenceBooksList {
        return ReferenceBooksList(referenceBookService.getAllReferenceBooks())
    }

    @GetMapping("get")
    fun get(@RequestParam(value = "aspectId", required = true) encodedAspectId: String): ReferenceBook {
        val aspectId = URLDecoder.decode(encodedAspectId, "UTF-8")
        return referenceBookService.getReferenceBook(aspectId)
    }

    @PostMapping("create")
    fun create(@RequestBody book: ReferenceBook): ReferenceBook {
        return referenceBookService.createReferenceBook(book.name, book.aspectId)
    }

    @PostMapping("update")
    fun update(@RequestBody book: ReferenceBook): ReferenceBook {
        return referenceBookService.updateReferenceBook(book)
    }

    @PostMapping("item/create")
    fun createItem(@RequestBody bookItem: ReferenceBookItem): ReferenceBook {
        return referenceBookService.addItemAndGetReferenceBook(bookItem)
    }

    @PostMapping("item/update")
    fun updateItem(@RequestBody bookItem: ReferenceBookItem): ReferenceBook {
        return referenceBookService.updateItemAndGetReferenceBook(bookItem)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<String> {
        return when (e) {
            is RefBookAlreadyExist -> ResponseEntity.badRequest().body("Aspect already has reference book")
            is RefBookNotExist -> ResponseEntity.badRequest().body("Aspect doesn't have reference book")
            is RefBookItemNotExist -> ResponseEntity.badRequest().body("Reference Book Item doesn't exist")
            is RefBookChildAlreadyExist -> ResponseEntity.badRequest().body("Reference Book Item '${e.value}' already exists")
            is RefBookAspectNotExist -> ResponseEntity.badRequest().body("Aspect doesn't exist")
            is RefBookItemMoveImpossible -> ResponseEntity.badRequest().body("Cannot move Reference Book Item")
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("${e.message}")
        }
    }
}