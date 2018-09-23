package il.ronmad.speedruntimer

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import il.ronmad.speedruntimer.realm.addCategory
import il.ronmad.speedruntimer.realm.addGame
import il.ronmad.speedruntimer.realm.addSplit
import il.ronmad.speedruntimer.realm.updateData
import il.ronmad.speedruntimer.web.SplitsIO
import io.realm.Realm
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplitsIOAndroidTests {

    private lateinit var realm: Realm

    @Before
    fun setUp() {
        Realm.init(InstrumentationRegistry.getContext())
        Realm.deleteRealm(Realm.getDefaultConfiguration()!!)
        realm = Realm.getDefaultInstance()
    }

    @After
    fun tearDown() {
        realm.close()
        Realm.deleteRealm(Realm.getDefaultConfiguration()!!)
    }

    @Test
    fun testToJson() {
        // application/splitsio
        val category = realm
                .addGame("TestGame")
                .addCategory("TestCategory")
        category.apply {
            addSplit("Test1").updateData(pbTime = 10000, bestTime = 9000)
            addSplit("Test2").updateData(pbTime = 25000, bestTime = 20000)
            addSplit("Test3").updateData(pbTime = 5000, bestTime = 4500)
            addSplit("Test4").updateData(pbTime = 100000, bestTime = 85000)
            updateData(runCount = 14)
        }

        val expectedJson = """{"_schemaVersion":"v1.0.0","timer":{"shortname":"fst","longname":"Floating Speedrun Timer","version":"v4.17"},"attempts":{"total":14},"game":{"longname":"TestGame"},"category":{"longname":"TestCategory"},"segments":[{"name":"Test1","endedAt":{"realtimeMS":10000},"bestDuration":{"realtimeMS":9000}},{"name":"Test2","endedAt":{"realtimeMS":35000},"bestDuration":{"realtimeMS":20000}},{"name":"Test3","endedAt":{"realtimeMS":40000},"bestDuration":{"realtimeMS":4500}},{"name":"Test4","endedAt":{"realtimeMS":140000},"bestDuration":{"realtimeMS":85000}}]}"""
        assertEquals(expectedJson, SplitsIO().serializeRun(category))
    }

    @Test
    fun testFromJson() {
        // application/splitsio
        val json = """{"_schemaVersion":"v1.0.0","timer":{"shortname":"fst","longname":"Floating Speedrun Timer","version":"v4.17"},"attempts":{"total":28},"game":{"longname":"Ori"},"category":{"longname":"100%"},"segments":[{"name":"Yes","endedAt":{"realtimeMS":59000},"bestDuration":{"realtimeMS":55000}},{"name":"Dude","endedAt":{"realtimeMS":131755},"bestDuration":{"realtimeMS":65000}}]}"""
        with(SplitsIO().deserializeRun(json)) {
            assertEquals("Ori", gameName)
            assertEquals("100%", name)
            assertEquals("Yes", splits[0]?.name)
            assertEquals(59000L, splits[0]?.pbTime)
            assertEquals(55000L, splits[0]?.bestTime)
            assertEquals("Dude", splits[1]?.name)
            assertEquals(131755L - 59000L, splits[1]?.pbTime)
            assertEquals(65000L, splits[1]?.bestTime)
            assertEquals(28, runCount)
        }
    }
}
