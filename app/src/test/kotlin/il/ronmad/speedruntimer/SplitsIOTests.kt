package il.ronmad.speedruntimer

import il.ronmad.speedruntimer.web.SplitsIO
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class SplitsIOTests {

    @Test
    fun testSerializeRun() {
        val run = SplitsIO.Run("TestGame", "TestCategory", 14,
                listOf(SplitsIO.Segment("Test1", 10000, 9000),
                        SplitsIO.Segment("Test2", 25000, 20000),
                        SplitsIO.Segment("Test3", 5000, 4500),
                        SplitsIO.Segment("Test4", 100000, 85000)
                ))

        val expectedJson = """{"_schemaVersion":"v1.0.0","timer":{"shortname":"fst","longname":"Floating Speedrun Timer","version":"v${BuildConfig.VERSION_NAME}"},"attempts":{"total":14},"game":{"longname":"TestGame"},"category":{"longname":"TestCategory"},"segments":[{"name":"Test1","endedAt":{"realtimeMS":10000},"bestDuration":{"realtimeMS":9000}},{"name":"Test2","endedAt":{"realtimeMS":35000},"bestDuration":{"realtimeMS":20000}},{"name":"Test3","endedAt":{"realtimeMS":40000},"bestDuration":{"realtimeMS":4500}},{"name":"Test4","endedAt":{"realtimeMS":140000},"bestDuration":{"realtimeMS":85000}}]}"""
        Assert.assertEquals(expectedJson, SplitsIO().serializeRun(run))
    }

    @Test
    fun testDeserializeRun() {
        val json = """{"_schemaVersion":"v1.0.0","timer":{"shortname":"fst","longname":"Floating Speedrun Timer","version":"v${BuildConfig.VERSION_NAME}"},"attempts":{"total":28},"game":{"longname":"Ori"},"category":{"longname":"100%"},"segments":[{"name":"Yes","endedAt":{"realtimeMS":59000},"bestDuration":{"realtimeMS":55000}},{"name":"Dude","endedAt":{"realtimeMS":131755},"bestDuration":{"realtimeMS":65000}}]}"""
        with(SplitsIO().deserializeRun(json)) {
            Assert.assertEquals("Ori", gameName)
            Assert.assertEquals("100%", categoryName)
            Assert.assertEquals("Yes", segments[0].segmentName)
            Assert.assertEquals(59000L, segments[0].pbDuration)
            Assert.assertEquals(55000L, segments[0].bestDuration)
            Assert.assertEquals("Dude", segments[1].segmentName)
            Assert.assertEquals(131755L - 59000L, segments[1].pbDuration)
            Assert.assertEquals(65000L, segments[1].bestDuration)
            Assert.assertEquals(28, attemptsTotal)
        }
    }

    @Test
    fun testGetAsnoobWR() {
        runBlocking {
            SplitsIO().getRun("2z69")?.let {
                assertEquals("Ori and the Blind Forest Definitive Edition", it.gameName)
                assertEquals("All Skills no OOB", it.categoryName)
                assertEquals(190359L, it.segments[0].pbDuration)
            } ?: fail("request failed")
        }
    }

    @Test
    fun testUploadRun() {
        val run = SplitsIO.Run("TestGame", "TestCategory", 14,
                listOf(SplitsIO.Segment("Test1", 10000, 9000),
                        SplitsIO.Segment("Test2", 25000, 20000),
                        SplitsIO.Segment("Test3", 5000, 4500),
                        SplitsIO.Segment("Test4", 100000, 85000)
                ))

        runBlocking {
            SplitsIO().uploadRun(run)?.let(::println) ?: fail("upload failed")
        }
    }
}
