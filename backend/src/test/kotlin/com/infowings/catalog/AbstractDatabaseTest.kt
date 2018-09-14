package com.infowings.catalog

import com.infowings.catalog.storage.OrientClass
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
abstract class AbstractDatabaseTest {

    @Autowired
    protected lateinit var orientDatabase: OrientDatabase

    @BeforeEach
    fun setupDatabase() {
        cleanupDatabase()
    }

    private fun cleanupDatabase() {
        transaction(orientDatabase) {
            orientDatabase.command("DELETE VERTEX ${OrientClass.ASPECT.extName}") {}
            orientDatabase.command("DELETE VERTEX ${OrientClass.ASPECT_PROPERTY.extName}") {}
            orientDatabase.command("DELETE VERTEX ${OrientClass.SUBJECT.extName}") {}
            orientDatabase.command("DELETE VERTEX ${OrientClass.OBJECT.extName}") {}
            orientDatabase.command("DELETE VERTEX ${OrientClass.OBJECT_PROPERTY.extName}") {}
            orientDatabase.command("DELETE VERTEX ${OrientClass.OBJECT_VALUE.extName}") {}
            orientDatabase.command("DELETE VERTEX ${OrientClass.REFBOOK_ITEM.extName}") {}
            orientDatabase.command("DELETE VERTEX ${OrientClass.HISTORY_ADD_LINK.extName}") {}
            orientDatabase.command("DELETE VERTEX ${OrientClass.HISTORY_ELEMENT.extName}") {}
            orientDatabase.command("DELETE VERTEX ${OrientClass.HISTORY_EVENT.extName}") {}
            orientDatabase.command("DELETE VERTEX ${OrientClass.HISTORY_REMOVE_LINK.extName}") {}
        }
    }
}