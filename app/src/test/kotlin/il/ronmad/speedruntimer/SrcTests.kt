package il.ronmad.speedruntimer

import com.google.common.collect.Lists
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import kotlin.test.*

class SrcTests {

    @Test
    @Throws(Exception::class)
    fun testGame140() {
        runBlocking {
            val api = Src().api
            val game140Res = api.game("140").awaitResult()
            when (game140Res) {
                is Result.Ok -> {
                    val game = game140Res.value
                    assertEquals("140", game.name)

                    assertEquals("Any%", game.categories[0].name)

                    assertTrue(game.categories[0].subCategories.isEmpty())
                }
                is Result.Error -> {
                    game140Res.exception.printStackTrace()
                    fail()
                }
                is Result.Exception -> {
                    game140Res.exception.printStackTrace()
                    fail()
                }
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGameThoth() {
        runBlocking {
            val api = Src().api
            val gameThothRes = api.game("Thoth").awaitResult()
            when (gameThothRes) {
                is Result.Ok -> {
                    val game = gameThothRes.value
                    assertEquals("THOTH", game.name)

                    assertEquals("Standard", game.categories[0].name)

                    assertEquals("Mode", game.categories[0].subCategories[0].name)

                    assertEquals("Solo", game.categories[0].subCategories[0].values[0].label)
                }
                is Result.Error -> {
                    gameThothRes.exception.printStackTrace()
                    fail()
                }
                is Result.Exception -> {
                    gameThothRes.exception.printStackTrace()
                    fail()
                }
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testLeaderboards() {
        runBlocking {
            val api = Src().api
            val gameRes = api.game("Thoth").awaitResult()
            when (gameRes) {
                is Result.Ok -> {
                    val game = gameRes.value
                    val leaderboards = game.categories.flatMap { category ->
                        if (category.leaderboardUrl != null) {
                            val url = category.leaderboardUrl!!
                            if (category.subCategories.isEmpty()) {
                                listOf(api.leaderboard(url))
                            } else {
                                val pairs = category.subCategories.map { variable ->
                                    variable.values.map {
                                        "var-${variable.id}" to it.id
                                    }
                                }
                                Lists.cartesianProduct(pairs).map {
                                    api.leaderboard(url, it.toMap())
                                }
                            }
                        }
                        else emptyList()
                    }
                    leaderboards.forEach {
                        val res = it.awaitResult()
                        when (res) {
                            is Result.Ok -> println(res.value.runs.size)
                            is Result.Error -> {
                                res.exception.printStackTrace()
                                fail()
                            }
                            is Result.Exception -> {
                                res.exception.printStackTrace()
                                fail()
                            }
                        }
                    }
                }
                is Result.Error -> {
                    gameRes.exception.printStackTrace()
                    fail()
                }
                is Result.Exception -> {
                    gameRes.exception.printStackTrace()
                    fail()
                }
            }
        }
    }
}
