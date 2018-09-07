package il.ronmad.speedruntimer

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import io.realm.Realm
import org.junit.After
import org.junit.Assert.*
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
        val category = realm
                .addGame("TestGame")
                .addCategory("TestCategory")
        category.addSplit("Test1")
                .updateData(pbTime = 10000, bestTime = 9000)
        category.addSplit("Test2")
                .updateData(pbTime = 25000, bestTime = 20000)
        category.addSplit("Test3")
                .updateData(pbTime = 5000, bestTime = 4500)
        category.addSplit("Test4")
                .updateData(pbTime = 100000, bestTime = 85000)

        val expectedJson = """{"_schemaVersion":"v1.0.0","timer":{"shortname":"fst","longname":"FloatingSpeedrunTimer","version":"v4.15"},"game":{"longname":"TestGame"},"category":{"longname":"TestCategory"},"segments":[{"name":"Test1","endedAt":{"realtimeMS":10000},"bestDuration":{"realtimeMS":9000}},{"name":"Test2","endedAt":{"realtimeMS":35000},"bestDuration":{"realtimeMS":20000}},{"name":"Test3","endedAt":{"realtimeMS":40000},"bestDuration":{"realtimeMS":4500}},{"name":"Test4","endedAt":{"realtimeMS":140000},"bestDuration":{"realtimeMS":85000}}]}"""
        assertEquals(expectedJson, SplitsIO().serializeRun(category))
    }

    @Test
    fun testFromJson() {
        val json = """{"_schemaVersion":"v1.0.0","timer":{"shortname":"fst","longname":"FloatingSpeedrunTimer","version":"v4.15"},"game":{"longname":"Ori"},"category":{"longname":"100%"},"segments":[{"name":"Yes","endedAt":{"realtimeMS":59000},"bestDuration":{"realtimeMS":55000}},{"name":"Dude","endedAt":{"realtimeMS":131755},"bestDuration":{"realtimeMS":65000}}]}"""
        val category = SplitsIO().deserializeRun(json)
        assertEquals("Ori", category.gameName)
        assertEquals("100%", category.name)
        assertEquals("Yes", category.splits[0]?.name)
        assertEquals(59000L, category.splits[0]?.pbTime)
        assertEquals(55000L, category.splits[0]?.bestTime)
        assertEquals("Dude", category.splits[1]?.name)
        assertEquals(131755L - 59000L, category.splits[1]?.pbTime)
        assertEquals(65000L, category.splits[1]?.bestTime)
    }
}
