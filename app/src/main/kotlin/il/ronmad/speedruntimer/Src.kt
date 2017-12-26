package il.ronmad.speedruntimer

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import retrofit2.Call
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import kotlin.math.roundToLong

interface SrcAPI {
    @GET("games")
    fun game(@Query("name") name: String): Call<SrcGame>

    @GET
    fun categories(@Url url: String): Call<Array<SrcCategory>>

    @GET
    fun leaderboard(@Url url: String): Call<SrcLeaderboard>

    @GET
    fun user(@Url url: String): Call<SrcUser>

    @GET("platforms/{id}")
    fun platform(@Path("id") id: String): Call<SrcPlatform>
}

data class SrcGame(val name: String, val links: List<SrcLink>)

data class SrcCategory(val name: String, val links: List<SrcLink>)

data class SrcLink(val rel: String?, val uri: String)

data class SrcLeaderboard(val weblink: String, val runs: List<SrcRun>) {

    var categoryName = ""
    var wrRunners = ""
    var wrPlatform = ""

    suspend fun initWrData(srcApi: SrcAPI) {
        if (runs.isEmpty()) return
        val wrRun = runs[0]
        wrRunners = try {
            wrRun.players.map {
                if (it.rel == "guest") it.name!!
                else {
                    (srcApi.user(it.uri).awaitResult() as Result.Ok)
                            .value.name
                }
            }.joinToString()
        } catch (e: Exception) { "[unknown]" }

        wrPlatform = try {
            (srcApi.platform(wrRun.platformId).awaitResult() as Result.Ok)
                    .value.name
        } catch (e: Exception) { "[unknown]" }
    }
}

data class SrcRun(val place: Int, val videoLink: SrcLink?, val players: List<SrcPlayer>,
                  val time: Long, val platformId: String)

data class SrcPlayer(val rel: String, val name: String?, val uri: String)

data class SrcUser(val name: String)

data class SrcPlatform(val name: String)

object Src {

    fun gsonConverter(): GsonConverterFactory {
        val gamesDeserializer = JsonDeserializer<SrcGame> { json, _, _ ->
            val gameArray = json.asJsonObject.get("data").asJsonArray
            val gameObj = gameArray[0].asJsonObject
            val name = gameObj.get("names").asJsonObject.get("international").asString
            val linksJson = gameObj.get("links").asJsonArray
            val links = GsonBuilder().create().fromJson(linksJson, Array<SrcLink>::class.java)
            SrcGame(name, links.toList())
        }

        val categoriesDeserializer = JsonDeserializer<Array<SrcCategory>> { json, _, _ ->
            val categoryArray = json.asJsonObject.get("data")
            GsonBuilder().create().fromJson(categoryArray, Array<SrcCategory>::class.java)
        }

        val leaderboardDeserializer = JsonDeserializer<SrcLeaderboard> { json, _, _ ->
            val gson = GsonBuilder().create()
            val leaderboardObj = json.asJsonObject.get("data").asJsonObject
            val weblink = leaderboardObj.get("weblink").asString
            val lbRuns = leaderboardObj.get("runs").asJsonArray
                    .map {
                        val lbRun = it.asJsonObject
                        val place = lbRun.get("place").asInt
                        val run = lbRun.get("run").asJsonObject

                        val videoLink = if (run.get("videos").isJsonNull) null
                        else
                            gson.fromJson(
                                run.get("videos").asJsonObject.get("links").asJsonArray[0],
                                SrcLink::class.java)
                        val players = gson.fromJson(run.get("players"),
                                Array<SrcPlayer>::class.java).toList()
                        val time = (run.get("times").asJsonObject
                                .get("primary_t").asFloat * 1000).roundToLong()
                        val platformId = run.get("system").asJsonObject.get("platform").asString
                        SrcRun(place, videoLink, players, time, platformId)
                    }
            SrcLeaderboard(weblink, lbRuns)
        }

        val platformDeserializer = JsonDeserializer<SrcPlatform> { json, _, _ ->
            val platformObj = json.asJsonObject.get("data").asJsonObject
            SrcPlatform(platformObj.get("name").asString)
        }

        val userDeserializer = JsonDeserializer<SrcUser> { json, _, _ ->
            val platformObj = json.asJsonObject.get("data").asJsonObject
            val name = platformObj.get("names").asJsonObject.get("international").asString
            SrcUser(name)
        }

        return GsonConverterFactory.create(GsonBuilder()
                .registerTypeAdapter(SrcGame::class.java, gamesDeserializer)
                .registerTypeAdapter(Array<SrcCategory>::class.java, categoriesDeserializer)
                .registerTypeAdapter(SrcLeaderboard::class.java, leaderboardDeserializer)
                .registerTypeAdapter(SrcPlatform::class.java, platformDeserializer)
                .registerTypeAdapter(SrcUser::class.java, userDeserializer)
                .create())
    }

    suspend fun fetchCategoriesForGame(context: Context, gameName: String): List<SrcCategory> {
        val defaultCategories: List<SrcCategory> = listOf()
        val srcApi = (context.applicationContext as MyApplication).srcApi
        return try {
            val game = (srcApi.game(gameName).awaitResult() as Result.Ok).value
            if (game.name.toLowerCase() != gameName.toLowerCase())
                defaultCategories
            else {
                val categoriesLink = game.links.find { it.rel == "categories" }
                if (categoriesLink == null)
                    defaultCategories
                else
                    (srcApi.categories(categoriesLink.uri).awaitResult() as Result.Ok).value.toList()
            }
        } catch (e: Exception) {
            defaultCategories
        }
    }

    suspend fun fetchLeaderboardsForGame(context: Context, gameName: String): List<SrcLeaderboard> {
        val defaultLeaderboards: List<SrcLeaderboard> = listOf()
        val srcApi = (context.applicationContext as MyApplication).srcApi
        return try {
            val categories = fetchCategoriesForGame(context, gameName)
            if (categories.isEmpty())
                defaultLeaderboards
            else {
                var leaderboards: List<SrcLeaderboard> = listOf()
                categories.forEach { category ->
                    category.links.find { it.rel == "leaderboard" }?.let {
                        val lbRes = srcApi.leaderboard(it.uri).awaitResult()
                        if (lbRes is Result.Ok) {
                            val lb = lbRes.value
                            lb.categoryName = category.name
                            lb.initWrData(srcApi)
                            leaderboards += lb
                        }
                    }
                }
                leaderboards
            }
        } catch (e: Exception) {
            defaultLeaderboards
        }
    }
}
