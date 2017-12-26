package il.ronmad.speedruntimer

import android.graphics.Color
import com.google.gson.*

import kotlinx.coroutines.experimental.runBlocking

import org.junit.Test

import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

import org.junit.Assert.*
import retrofit2.Retrofit
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class UnitTests {
    @Test
    @Throws(Exception::class)
    fun testTimeSplit() {
        val bestTime = (43 + 1000 * 27 + 1000 * 60 * 5).toLong()
        val hours = (bestTime / (3600 * 1000)).toInt()
        var remaining = (bestTime % (3600 * 1000)).toInt()
        val minutes = remaining / (60 * 1000)
        remaining %= (60 * 1000)
        val seconds = remaining / 1000
        val milliseconds = remaining % 1000

        assertEquals(0, hours.toLong())
        assertEquals(5, minutes.toLong())
        assertEquals(27, seconds.toLong())
        assertEquals(43, milliseconds.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testAlpha() {
        println(Color.alpha(Color.argb(255, 255, 255, 255)))
    }

    @Test
    @Throws(Exception::class)
    fun testSpeedrunComCategories() {
        val srcApi = "https://www.speedrun.com/api/v1"
        val gameName = URLEncoder.encode("Monument Valley", "UTF-8")
        var u = URL(srcApi + "/games?name=" + gameName)
        var conn = u.openConnection() as HttpURLConnection
        var reader = InputStreamReader(conn.inputStream)
        var json = JsonParser().parse(reader).asJsonObject
        val gameData = json.get("data").asJsonArray.get(0).asJsonObject
        val id = gameData.get("id").asString

        u = URL("$srcApi/games/$id/categories")
        conn = u.openConnection() as HttpURLConnection
        reader = InputStreamReader(conn.inputStream)
        json = JsonParser().parse(reader).asJsonObject
        val categoriesData = json.get("data").asJsonArray
        for (categoryElement in categoriesData) {
            val categoryObject = categoryElement.asJsonObject
            println(categoryObject.get("name").asString)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSrcRetrofit() {
        val api = Retrofit.Builder()
                .addConverterFactory(Src.gsonConverter())
                .baseUrl(SRC_API)
                .build()
                .create(SrcAPI::class.java)

        runBlocking {
            try {
                val resp140 = api.game("140").awaitResult() as Result.Ok
                val game140 = resp140.value
                assertEquals("140", game140.name)

                val respCategories = api.categories(game140.links.find {
                    it.rel == "categories"
                }!!.uri).awaitResult() as Result.Ok
                println(respCategories.value.map { it.name }.joinToString())
                val anyp = respCategories.value[0]
                assertEquals("Any%", anyp.name)

                val respLeaderboard = api.leaderboard(anyp.links.find {
                    it.rel == "leaderboard"
                }!!.uri).awaitResult() as Result.Ok
                val wrRun = respLeaderboard.value.runs[0]
                assertEquals(967450.toLong(), wrRun.time)

                val player = wrRun.players[0]
                val name = if (player.rel == "guest") player.name!!
                else {
                    val respUser = api.user(player.uri).awaitResult() as Result.Ok
                    respUser.value.name
                }
                assertEquals("Zet", name)

                val respPlatform = api.platform(wrRun.platformId).awaitResult() as Result.Ok
                assertEquals("PC", respPlatform.value.name)
            } catch (e: Exception) {
                fail(e.message)
            }
        }
    }
}
