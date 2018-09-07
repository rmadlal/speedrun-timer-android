package il.ronmad.speedruntimer

import com.google.gson.internal.LazilyParsedNumber
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.test.TestCoroutineContext
import org.junit.Test
import kotlin.test.*

class SplitsIOTests {

    @Test
    fun testAsnoobWR() {
        val api = SplitsIO()
        runBlocking(context = TestCoroutineContext("SplitsIOTest")) {
            val run = api.getRun("2z69") ?: fail("request failed")
            assertEquals("2z69", run.id)
            assertEquals("Ori and the Blind Forest Definitive Edition", run.gameName)
            assertEquals("All Skills no OOB", run.categoryName)
            assertEquals(1690799, (run.realtime_duration_ms as LazilyParsedNumber).toInt())
            assertEquals(190359, (run.segments[0].realtime_duration_ms as LazilyParsedNumber).toInt())
        }
    }
}
