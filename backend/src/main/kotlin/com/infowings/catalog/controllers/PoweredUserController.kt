package com.infowings.catalog.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/powereduser")
class PoweredUserController {

    @GetMapping
    fun testPoweredUser(): String {
        return "Message for powered user"
    }
}