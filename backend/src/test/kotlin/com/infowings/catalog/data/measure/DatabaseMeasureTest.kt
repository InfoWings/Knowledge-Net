package com.infowings.catalog.data.measure

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.data.LengthGroup
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.SpeedGroup
import com.infowings.catalog.storage.OrientDatabase
import com.orientechnologies.orient.core.record.ODirection
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner


@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DatabaseMeasureTest {

    @Autowired
    lateinit var measureService: MeasureService

    @Autowired
    lateinit var database: OrientDatabase

    @Test
    fun lengthGroupExist() = database.acquire().use {
        assertTrue("Length group must exist", measureService.findMeasureGroup(LengthGroup.name, it) != null)
    }

    @Test
    fun speedGroupExist() = database.acquire().use {
        assertTrue("Speed group must exist", measureService.findMeasureGroup(SpeedGroup.name, it) != null)
    }

    @Test
    fun lengthGroupBaseMeasureExist() = database.acquire().use {
        val baseVertex = measureService.findMeasure(LengthGroup.base.name, it)
        val groupVertex = measureService.findMeasureGroup(LengthGroup.name, it)
        assertTrue("Length base measure must exist", baseVertex != null)
        assertTrue("Length base measure must be linked with Length group",
                baseVertex!!.getVertices(ODirection.BOTH).contains(groupVertex!!))
    }


    @Test
    fun speedGroupBaseMeasureExist() = database.acquire().use {
        val baseVertex = measureService.findMeasure(SpeedGroup.base.name, it)
        val groupVertex = measureService.findMeasureGroup(SpeedGroup.name, it)
        assertTrue("Speed base measure must exist", baseVertex != null)
        assertTrue("Speed base measure must be linked with Speed group",
                baseVertex!!.getVertices(ODirection.BOTH).contains(groupVertex!!))
    }

    @Test
    fun lengthGroupContainsAllTheirMeasures() = database.acquire().use { db ->
        val baseVertex = measureService.findMeasure(LengthGroup.base.name, db)
        LengthGroup.measureList.forEach {
            assertTrue("Measure ${it.name} must exist", measureService.findMeasure(it.name, db) != null)
            assertTrue("Measure ${it.name} must be linked with ${LengthGroup.base.name}",
                    measureService.findMeasure(it.name, db)!!.getVertices(ODirection.OUT).contains(baseVertex!!))
        }
    }

    @Test
    fun speedGroupContainsAllTheirMeasures() = database.acquire().use { db ->
        val baseVertex = measureService.findMeasure(SpeedGroup.base.name, db)
        SpeedGroup.measureList.forEach {
            assertTrue("Measure ${it.name} must exist", measureService.findMeasure(it.name, db) != null)
            assertTrue("Measure ${it.name} must be linked with ${SpeedGroup.base.name}",
                    measureService.findMeasure(it.name, db)!!.getVertices(ODirection.OUT).contains(baseVertex!!))
        }
    }

    @Test
    fun measureDependencies() = database.acquire().use {
        val lengthGroupVertex = measureService.findMeasureGroup(LengthGroup.name, it)
        val speedGroupVertex = measureService.findMeasureGroup(SpeedGroup.name, it)
        assertTrue("Length group must be linked with Speed group",
                lengthGroupVertex!!.getVertices(ODirection.BOTH).contains(speedGroupVertex!!))
    }
}