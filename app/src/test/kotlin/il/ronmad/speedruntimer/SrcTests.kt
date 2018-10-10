package il.ronmad.speedruntimer

import il.ronmad.speedruntimer.web.Failure
import il.ronmad.speedruntimer.web.Src
import il.ronmad.speedruntimer.web.Success
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class SrcTests {

    @Test
    fun testGame140() {
        runBlocking {
            when (val game140Res = Src().fetchGameData("140")) {
                is Success -> {
                    with(game140Res.value) {
                        assertEquals("140", name)
                        assertEquals("Any%", categories[0].name)
                        assertTrue(categories[0].subCategories.isEmpty())
                    }
                }
                is Failure -> fail()
            }
        }
    }

    @Test
    fun testGameThoth() {
        runBlocking {
            when (val gameThothRes = Src().fetchGameData("Thoth")) {
                is Success -> {
                    with(gameThothRes.value) {
                        assertEquals("THOTH", name)
                        assertEquals("Standard", categories[0].name)
                        assertEquals("Mode", categories[0].subCategories[0].name)
                        assertEquals("Solo", categories[0].subCategories[0].values[0].label)
                    }
                }
                is Failure -> fail()
            }
        }
    }

    @Test
    fun testLeaderboards() {
        runBlocking {
            when (val leaderboardRes = Src().fetchLeaderboardsForGame("Thoth")) {
                is Success -> {
                    with(leaderboardRes.value) {
                        assertEquals(3, size)
                        assertEquals(13, get(0).runs.size)
                        assertEquals("Procedural", get(1).categoryName)
                        assertEquals(776992, get(2).runs[0].time)
                    }
                }
                is Failure -> fail()
            }
        }
    }
}
