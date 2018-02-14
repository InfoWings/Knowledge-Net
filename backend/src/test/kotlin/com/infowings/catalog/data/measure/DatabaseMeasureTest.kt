package com.infowings.catalog.data.measure

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.data.BaseMeasureUnit
import com.infowings.catalog.data.LengthMeasure
import com.infowings.catalog.data.SpeedMeasure
import com.infowings.catalog.data.restoreMeasureUnit
import com.infowings.catalog.storage.MEASURE_EDGE_CLASS
import com.infowings.catalog.storage.MEASURE_VERTEX_CLASS
import com.infowings.catalog.storage.OrientDatabase
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.util.stream.Collectors


@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DatabaseMeasureTest {

    @Autowired
    lateinit var database: OrientDatabase

    @Test
    fun findLengthMeasureDependencies() {
        findMeasureDependencies(LengthMeasure)
    }

    @Test
    fun findSpeedMeasureDependencies() {
        findMeasureDependencies(SpeedMeasure)
    }

    private fun findMeasureDependencies(baseUnit: BaseMeasureUnit<*, *>) {
        val query = "SELECT expand(out('$MEASURE_EDGE_CLASS')) from $MEASURE_VERTEX_CLASS where name = ?"
        database.acquire().query(query, baseUnit.toString()).use {

            val dependenciesFromBd = it.elementStream()
                    .map { restoreMeasureUnit(it.getProperty<String>("name")) }
                    .collect(Collectors.toSet())

            assertThat("Size of dependency set of measure should be the same in database and in memory",
                    baseUnit.linkedTypes.size,
                    equalTo(dependenciesFromBd.size))

            for (type in baseUnit.linkedTypes) {
                assertTrue("Dependency of measure in memory " +
                        "should be included in the set of dependencies of measure in database",
                        dependenciesFromBd.contains(type))
            }
        }
    }
}