package com.infowings.catalog.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class AdminController {

    @GetMapping
    fun testAdmin(): String {
        return "Message for admin"
    }
}