package com.infowings.catalog.external

import com.infowings.catalog.common.BadRequest
import com.infowings.catalog.common.BadRequestCode.NEED_CONFIRMATION
import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.ReferenceBooksList
import com.infowings.catalog.data.aspect.AspectDoesNotExist
import com.infowings.catalog.data.reference.book.*
import kotlinx.serialization.json.JSON
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.security.Principal

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
    fun create(@RequestBody book: ReferenceBook, principal: Principal): ReferenceBook {
        return referenceBookService.createReferenceBook(book.name, book.aspectId, principal.name)
    }

    @PostMapping("update")
    fun update(@RequestBody book: ReferenceBook, principal: Principal) {
        referenceBookService.updateReferenceBook(book, principal.name)
    }

    @PostMapping("remove")
    fun remove(@RequestBody book: ReferenceBook, principal: Principal) {
        referenceBookService.removeReferenceBook(book, principal.name)
    }

    @PostMapping("forceRemove")
    fun forceRemove(@RequestBody book: ReferenceBook, principal: Principal) {
        referenceBookService.removeReferenceBook(book, principal.name, true)
    }

    @PostMapping("item/create")
    fun createItem(@RequestBody bookItem: ReferenceBookItem, principal: Principal) {
        referenceBookService.addReferenceBookItem(bookItem, principal.name)
    }

    @PostMapping("item/update")
    fun updateItem(@RequestBody bookItem: ReferenceBookItem, principal: Principal) {
        referenceBookService.changeValue(bookItem, principal.name)
    }

    @PostMapping("item/forceUpdate")
    fun forceUpdateItem(@RequestBody bookItem: ReferenceBookItem, principal: Principal) {
        referenceBookService.changeValue(bookItem, principal.name, true)
    }

    @PostMapping("item/remove")
    fun removeItem(@RequestBody bookItem: ReferenceBookItem, principal: Principal) {
        referenceBookService.removeReferenceBookItem(bookItem, principal.name)
    }

    @PostMapping("item/forceRemove")
    fun forceRemoveItem(@RequestBody bookItem: ReferenceBookItem, principal: Principal) {
        referenceBookService.removeReferenceBookItem(bookItem, principal.name, true)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<String> {
        return when (e) {
            is RefBookAlreadyExist -> ResponseEntity.badRequest().body("Aspect already has reference book")
            is RefBookNotExist -> ResponseEntity.badRequest().body("Aspect doesn't have reference book")
            is RefBookItemNotExist -> ResponseEntity.badRequest().body("Reference Book Item doesn't exist")
            is RefBookChildAlreadyExist -> ResponseEntity.badRequest().body("Reference Book Item '${e.value}' already exists")
            is AspectDoesNotExist -> ResponseEntity.badRequest().body("Aspect doesn't exist")
            is RefBookItemMoveImpossible -> ResponseEntity.badRequest().body("Cannot move Reference Book Item")
            is RefBookItemIllegalArgumentException -> ResponseEntity.badRequest().body(e.message)
            is RefBookItemHasLinkedEntitiesException -> ResponseEntity.badRequest().body(
                JSON.stringify(
                    BadRequest(
                        NEED_CONFIRMATION,
                        "These Reference Book Items has linked entities: ${e.itemsWithLinkedObjects.map { it.value }}"
                    )
                )
            )
            is RefBookItemConcurrentModificationException ->
                ResponseEntity.badRequest().body("Attempt to modify old version of Reference Book Item. Please refresh page.")
            is RefBookConcurrentModificationException ->
                ResponseEntity.badRequest().body("Attempt to modify old version of Reference Book. Please refresh page.")
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message)
        }
    }
}