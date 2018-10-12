package il.ronmad.speedruntimer.web

import com.google.common.collect.Lists
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializer
import il.ronmad.speedruntimer.SRC_API
import il.ronmad.speedruntimer.isEmpty
import il.ronmad.speedruntimer.toResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import kotlin.math.roundToLong

interface SrcAPI {
    @GET("games")
    fun game(
            @Query("name") name: String,
            @Query("embed") embed: String = "categories.variables"
    ): Call<SrcGame>

    @GET()
    fun leaderboard(
            @Url url: String,
            @QueryMap variables: Map<String, String> = emptyMap()
    ): Call<SrcLeaderboard>

    @GET
    fun user(@Url url: String): Call<SrcUser>

    @GET("platforms/{id}")
    fun platform(@Path("id") id: String): Call<SrcPlatform>
}

data class SrcGame(
        val name: String,
        val categories: List<SrcCategory>,
        val links: List<SrcLink>
) {

    companion object {

        val EMPTY_GAME = SrcGame("", emptyList(), emptyList())
    }
}

data class SrcCategory(
        val name: String,
        val subCategories: List<SrcVariable>,
        val leaderboardUrl: String?
)

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
                withContext(Dispatchers.IO) {
                    srcApi.user(it.uri).execute()
                }.body()?.let { user ->
                    user.name
                } ?: "[unknown]"
            }
        }.joinToString()

        wrPlatform = wrRun.platformId?.let { platformId ->
            withContext(Dispatchers.IO) {
                srcApi.platform(platformId).execute()
            }.body()?.let { it.name }
        } ?: "[unknown]"
    }
}

data class SrcRun(
        val place: Int,
        val videoLink: SrcLink?,
        val players: List<SrcPlayer>,
        val time: Long, val platformId: String?
)

data class SrcPlayer(val rel: String, val name: String?, val uri: String)

data class SrcUser(val name: String)

data class SrcPlatform(val name: String)

data class SrcVariable(val id: String, val name: String, val values: List<SrcValue>)

data class SrcValue(val id: String, val label: String)

class Src private constructor() {

    // If application was not passed, there will be no caching.

    private val gson = setupGson()
    private val api = setupApi()

    private var gameCache: Map<String, SrcGame> = emptyMap()

    private fun setupGson(): Gson {
        fun variablesDeserializer(json: JsonArray): List<SrcVariable> {
            return json
                    .filter {
                        val variableObj = it.asJsonObject
                        variableObj.get("is-subcategory").asBoolean
                    }
                    .map { variableElement ->
                        val variableObj = variableElement.asJsonObject
                        val id = variableObj.get("id").asString
                        val name = variableObj.get("name").asString
                        val valuesObj = variableObj.getAsJsonObject("values").getAsJsonObject("values")
                        val values = valuesObj.entrySet().map {
                            val label = it.value.asJsonObject.get("label").asString
                            SrcValue(it.key, label)
                        }
                        SrcVariable(id, name, values)
                    }
        }

        fun categoriesDeserializer(json: JsonArray): List<SrcCategory> {
            return json
                    .map { categoryElement ->
                        val categoryObj = categoryElement.asJsonObject
                        val name = categoryObj.get("name").asString
                        val subCategories = variablesDeserializer(
                                categoryObj.getAsJsonObject("variables").getAsJsonArray("data"))
                        val links = Gson().fromJson(categoryObj.get("links"), Array<SrcLink>::class.java)
                        val leaderboardLink = links.find { it.rel == "leaderboard" }?.uri
                        SrcCategory(name, subCategories, leaderboardLink)
                    }
        }

        val gamesDeserializer = JsonDeserializer<SrcGame> { json, _, _ ->
            val gameArray = json.asJsonObject.getAsJsonArray("data")
            if (gameArray.isEmpty())
                return@JsonDeserializer SrcGame.EMPTY_GAME
            val gameObj = gameArray[0].asJsonObject
            val name = gameObj.getAsJsonObject("names").get("international").asString
            val categories = categoriesDeserializer(gameObj.getAsJsonObject("categories").getAsJsonArray("data"))
            val links = Gson().fromJson(gameObj.get("links"), Array<SrcLink>::class.java)
            SrcGame(name, categories, links.toList())
        }

        val leaderboardDeserializer = JsonDeserializer<SrcLeaderboard> { json, _, _ ->
            val gson = Gson()
            val leaderboardObj = json.asJsonObject.getAsJsonObject("data")
            val weblink = leaderboardObj.get("weblink").asString
            val lbRuns = leaderboardObj.getAsJsonArray("runs").map {
                val lbRun = it.asJsonObject
                val place = lbRun.get("place").asInt
                val run = lbRun.getAsJsonObject("run")

                val videoLink = when {
                    run.get("videos").isJsonNull -> null
                    run.getAsJsonObject("videos").get("links") == null -> null
                    run.getAsJsonObject("videos").get("links").isJsonNull -> null
                    run.getAsJsonObject("videos").getAsJsonArray("links").isEmpty() -> null
                    else -> gson.fromJson(
                            run.getAsJsonObject("videos").getAsJsonArray("links")[0],
                            SrcLink::class.java)
                }
                val players = gson.fromJson(run.get("players"),
                        Array<SrcPlayer>::class.java).toList()
                val time = (run.getAsJsonObject("times")
                        .get("primary_t").asFloat * 1000).roundToLong()
                val platform = run.getAsJsonObject("system").get("platform")
                val platformId = if (platform.isJsonNull) null else platform.asString
                SrcRun(place, videoLink, players, time, platformId)
            }
            SrcLeaderboard(weblink, lbRuns)
        }

        val userDeserializer = JsonDeserializer<SrcUser> { json, _, _ ->
            val platformObj = json.asJsonObject.getAsJsonObject("data")
            val name = platformObj.getAsJsonObject("names").get("international").asString
            SrcUser(name)
        }

        val platformDeserializer = JsonDeserializer<SrcPlatform> { json, _, _ ->
            SrcPlatform(json.asJsonObject.getAsJsonObject("data").get("name").asString)
        }

        return GsonBuilder()
                .registerTypeAdapter(SrcGame::class.java, gamesDeserializer)
                .registerTypeAdapter(SrcLeaderboard::class.java, leaderboardDeserializer)
                .registerTypeAdapter(SrcUser::class.java, userDeserializer)
                .registerTypeAdapter(SrcPlatform::class.java, platformDeserializer)
                .create()
    }

    private fun setupApi(): SrcAPI {
        return Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(SRC_API)
                .build()
                .create(SrcAPI::class.java)
    }

    suspend fun fetchGameData(gameName: String): Result<SrcGame> {
        return gameCache.getOrElse(gameName) {
            withContext(Dispatchers.IO) {
                api.game(gameName).execute()
            }.body()?.takeIf { it.name.toLowerCase() == gameName.toLowerCase() }
                    ?.also { gameCache += gameName to it }
        }.toResult()
    }

    suspend fun fetchLeaderboardsForGame(gameName: String): Result<List<SrcLeaderboard>> {
        return when (val game = fetchGameData(gameName)) {
            is Success -> {
                try {
                    game.value.categories.flatMap { category ->
                        if (category.leaderboardUrl == null) return@flatMap emptyList<SrcLeaderboard>()
                        if (category.subCategories.isEmpty()) {
                            withContext(Dispatchers.IO) {
                                api.leaderboard(category.leaderboardUrl).execute()
                            }.body()?.run {
                                categoryName = category.name
                                initWrData(api)
                                listOf(this)
                            } ?: emptyList()
                        } else {
                            val pairs = category.subCategories.map { variable ->
                                variable.values.map { variable to it }
                            }
                            Lists.cartesianProduct(pairs).mapNotNull { varValPairList ->
                                val varQuery: Map<String, String> = varValPairList.map {
                                    "var-${it.first.id}" to it.second.id
                                }.toMap()
                                withContext(Dispatchers.IO) {
                                    api.leaderboard(category.leaderboardUrl, varQuery).execute()
                                }.body()?.apply {
                                    categoryName = category.name
                                    subcategories = varValPairList.map { it.second.label }
                                    initWrData(api)
                                }
                            }
                        }
                    }.toResult()
                } catch (e: OutOfMemoryError) {
                    Failure<List<SrcLeaderboard>>()
                }
            }
            is Failure -> Failure()
        }
    }

    companion object {

        val instance by lazy { Src() }

        operator fun invoke() = instance
    }
}
