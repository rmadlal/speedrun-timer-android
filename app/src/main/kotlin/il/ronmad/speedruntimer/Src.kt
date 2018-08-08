package il.ronmad.speedruntimer

import android.content.Context
import com.google.common.collect.Lists
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializer
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import kotlin.math.roundToLong

interface SrcAPI {
    @GET("games")
    fun game(@Query("name") name: String,
             @Query("embed") embed: String = "categories.variables"): Call<SrcGame>

    @GET()
    fun leaderboard(@Url url: String,
                    @QueryMap variables: Map<String, String> = emptyMap()): Call<SrcLeaderboard>

    @GET
    fun user(@Url url: String): Call<SrcUser>

    @GET("platforms/{id}")
    fun platform(@Path("id") id: String): Call<SrcPlatform>
}

data class SrcGame(val name: String, val categories: List<SrcCategory>, val links: List<SrcLink>) {
    companion object {
        val EMPTY_GAME = SrcGame("", emptyList(), emptyList())
    }
}

data class SrcCategory(val name: String, val subCategories: List<SrcVariable>,
                       val leaderboardUrl: String?)

data class SrcLink(val rel: String?, val uri: String)

data class SrcLeaderboard(val weblink: String, val runs: List<SrcRun>) {

    var categoryName = ""
    var subcategories: List<String> = listOf()
    var wrRunners = ""
    var wrPlatform = ""

    suspend fun initWrData(srcApi: SrcAPI) {
        if (runs.isEmpty()) return
        val wrRun = runs[0]
        wrRunners = wrRun.players.map {
            if (it.rel == "guest") it.name!!
            else {
                val userRes = srcApi.user(it.uri).awaitResult()
                when (userRes) {
                    is Result.Ok -> userRes.value.name
                    else -> "[unknown]"
                }
            }
        }.joinToString()

        val platformRes = srcApi.platform(wrRun.platformId).awaitResult()
        wrPlatform = when (platformRes) {
            is Result.Ok -> platformRes.value.name
            else -> "[unknown]"
        }
    }
}

data class SrcRun(val place: Int, val videoLink: SrcLink?, val players: List<SrcPlayer>,
                  val time: Long, val platformId: String)

data class SrcPlayer(val rel: String, val name: String?, val uri: String)

data class SrcUser(val name: String)

data class SrcPlatform(val name: String)

data class SrcVariable(val id: String, val name: String, val values: List<SrcValue>)

data class SrcValue(val id: String, val label: String)

object Src {

    fun srcAPI(): SrcAPI {
        return Retrofit.Builder()
                .addConverterFactory(Src.gsonConverter())
                .baseUrl(SRC_API)
                .build()
                .create(SrcAPI::class.java)
    }

    private fun categoriesDeserializer(json: JsonArray): List<SrcCategory> {
        return json
                .map { categoryElement ->
                    val categoryObj = categoryElement.asJsonObject
                    val name = categoryObj.get("name").asString
                    val subCategories = variablesDeserializer(
                            categoryObj.get("variables").asJsonObject.get("data").asJsonArray)
                    val links = GsonBuilder().create()
                            .fromJson(categoryObj.get("links"), Array<SrcLink>::class.java)
                    val leaderboardLink = links.find { it.rel == "leaderboard" }?.uri
                    SrcCategory(name, subCategories, leaderboardLink)
                }
    }

    private fun variablesDeserializer(json: JsonArray): List<SrcVariable> {
        return json
                .filter {
                    val variableObj = it.asJsonObject
                    variableObj.get("is-subcategory").asBoolean
                }
                .map { variableElement ->
                    val variableObj = variableElement.asJsonObject
                    val id = variableObj.get("id").asString
                    val name = variableObj.get("name").asString
                    val valuesObj = variableObj.get("values").asJsonObject.get("values").asJsonObject
                    val values = valuesObj.entrySet().map {
                        val label = it.value.asJsonObject.get("label").asString
                        SrcValue(it.key, label)
                    }
                    SrcVariable(id, name, values)
                }
    }

    fun gsonConverter(): GsonConverterFactory {

        val gamesDeserializer = JsonDeserializer<SrcGame> { json, _, _ ->
            val gameArray = json.asJsonObject.get("data").asJsonArray
            val gameObj = gameArray[0].asJsonObject
            val name = gameObj.get("names").asJsonObject.get("international").asString
            val categories = categoriesDeserializer(gameObj.get("categories").asJsonObject.get("data").asJsonArray)
            val links = GsonBuilder().create().fromJson(gameObj.get("links"), Array<SrcLink>::class.java)
            SrcGame(name, categories, links.toList())
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

        val userDeserializer = JsonDeserializer<SrcUser> { json, _, _ ->
            val platformObj = json.asJsonObject.get("data").asJsonObject
            val name = platformObj.get("names").asJsonObject.get("international").asString
            SrcUser(name)
        }

        val platformDeserializer = JsonDeserializer<SrcPlatform> { json, _, _ ->
            SrcPlatform(json.asJsonObject.get("data").asJsonObject.get("name").asString)
        }

        return GsonConverterFactory.create(GsonBuilder()
                .registerTypeAdapter(SrcGame::class.java, gamesDeserializer)
                .registerTypeAdapter(SrcLeaderboard::class.java, leaderboardDeserializer)
                .registerTypeAdapter(SrcUser::class.java, userDeserializer)
                .registerTypeAdapter(SrcPlatform::class.java, platformDeserializer)
                .create())
    }

    suspend fun fetchGameData(context: Context?, gameName: String): SrcGame {
        val app = context?.applicationContext as? MyApplication ?: return SrcGame.EMPTY_GAME
        return app.srcGameCache.getOrElse(gameName) {
            val gameRes = app.srcApi.game(gameName).awaitResult()
            when (gameRes) {
                is Result.Ok -> {
                    val game = gameRes.value
                    if (game.name.toLowerCase() == gameName.toLowerCase()) {
                        app.srcGameCache += gameName to game
                        game
                    } else SrcGame.EMPTY_GAME
                }
                else -> SrcGame.EMPTY_GAME
            }
        }
    }

    suspend fun fetchLeaderboardsForGame(context: Context?, gameName: String): List<SrcLeaderboard> {
        val app = context?.applicationContext as? MyApplication ?: return emptyList()
        val game = fetchGameData(context, gameName)
        return if (game == SrcGame.EMPTY_GAME) emptyList()
        else try {
            game.categories.flatMap { category ->
                if (category.leaderboardUrl == null) return@flatMap emptyList<SrcLeaderboard>()
                if (category.subCategories.isEmpty()) {
                    val leaderboardRes = app.srcApi.leaderboard(category.leaderboardUrl).awaitResult()
                    when (leaderboardRes) {
                        is Result.Ok -> {
                            val leaderboard = leaderboardRes.value
                            leaderboard.categoryName = category.name
                            leaderboard.initWrData(app.srcApi)
                            listOf(leaderboard)
                        }
                        else -> emptyList()
                    }
                } else {
                    val pairs = category.subCategories.map { variable ->
                        variable.values.map { variable to it }
                    }
                    Lists.cartesianProduct(pairs).mapNotNull { varValPairList ->
                        val varQuery: Map<String, String> = varValPairList.map {
                            "var-${it.first.id}" to it.second.id
                        }.toMap()
                        val leaderboardRes = app.srcApi.leaderboard(category.leaderboardUrl, varQuery).awaitResult()
                        when (leaderboardRes) {
                            is Result.Ok -> {
                                val leaderboard = leaderboardRes.value
                                leaderboard.categoryName = category.name
                                leaderboard.subcategories = varValPairList.map { it.second.label }
                                leaderboard.initWrData(app.srcApi)
                                leaderboard
                            }
                            else -> null
                        }
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            emptyList<SrcLeaderboard>()
        }
    }
}
