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

    @PostMapping("remove")
    fun remove(@RequestBody book: ReferenceBook) {
        referenceBookService.removeReferenceBook(book)
    }

    @PostMapping("forceRemove")
    fun forceRemove(@RequestBody book: ReferenceBook) {
        referenceBookService.removeReferenceBook(book, true)
    }

    @PostMapping("item/create")
    fun createItem(@RequestBody bookItem: ReferenceBookItem) {
        referenceBookService.addReferenceBookItem(bookItem)
    }

    @PostMapping("item/update")
    fun updateItem(@RequestBody bookItem: ReferenceBookItem) {
        referenceBookService.changeValue(bookItem)
    }

    @PostMapping("item/remove")
    fun removeItem(@RequestBody bookItem: ReferenceBookItem) {
        referenceBookService.removeReferenceBookItem(bookItem)
    }

    @PostMapping("item/forceRemove")
    fun forceRemoveItem(@RequestBody bookItem: ReferenceBookItem) {
        referenceBookService.removeReferenceBookItem(bookItem, true)
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
            is RefBookModificationException -> ResponseEntity.badRequest().body("Cannot find parent Reference Book Item")
            is RefBookItemHasLinkedEntitiesException -> ResponseEntity.badRequest().body("Reference Book Item has linked entities")
            is RefBookHasLinkedEntitiesException -> ResponseEntity.badRequest().body("Reference Book has linked entities")
            is RefBookItemConcurrentModificationException ->
                ResponseEntity.badRequest().body("Attempt to modify old version of Reference Book Item. Please refresh page.")
            is RefBookConcurrentModificationException ->
                ResponseEntity.badRequest().body("Attempt to modify old version of Reference Book. Please refresh page.")
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("${e.message}")
        }
    }
}