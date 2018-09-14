package il.ronmad.speedruntimer.web

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import il.ronmad.speedruntimer.BuildConfig
import il.ronmad.speedruntimer.realm.*
import io.realm.Realm
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult
import java.io.File

const val BASE_URL = "https://splits.io/api/v4/"

interface SplitsIOAPI {
    @GET("runs/{id}")
    fun run(@Path("id") id: String): Call<SplitsIORun>

    @POST("runs")
    fun requestUploadRun(): Call<SplitsIOUploadRequest>


    @Multipart
    @POST
    fun uploadRun(@Url claimUri: String,
                  @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>): Call<ResponseBody>
}

data class SplitsIORun(val id: String,
                       val realtime_duration_ms: Number,
                       val realtime_sum_of_best_ms: Number,
                       val attempts: Number,
                       val game: Map<String, Any>,
                       val category: Map<String, Any>,
                       val segments: List<SplitsIOSegment>) {
    val gameName
        get() = game["name"] as String

    val categoryName
        get() = category["name"] as String
}

data class SplitsIOSegment(val id: String,
                           val name: String,
                           val segment_number: Number,
                           val realtime_start_ms: Number,
                           val realtime_end_ms: Number,
                           val realtime_duration_ms: Number,
                           val realtime_shortest_duration_ms: Number)

data class SplitsIOUploadRequest(val claimUri: String,
                                 val uploadUri: String,
                                 val fields: Map<String, String>)

class SplitsIO {

    private val gson = setupGson()
    val api = setupApi()

    private fun setupGson(): Gson {
        val runDeserializer = JsonDeserializer<SplitsIORun> { json, _, _ ->
            val run = json.asJsonObject.get("run")
            Gson().fromJson(run, SplitsIORun::class.java)
        }

        val runUploadRequestDeserializer = JsonDeserializer<SplitsIOUploadRequest> { json, _, _ ->
            val claimUri = json.asJsonObject
                    .get("uris").asJsonObject
                    .get("claim_uri").asString
            val presignedRequest = json.asJsonObject
                    .get("presigned_request").asJsonObject
            val uploadUri = presignedRequest
                    .get("uri").asString
            val fields = Gson().fromJson<Map<String, String>>(
                    presignedRequest.get("fields").asJsonObject,
                    object : TypeToken<Map<String, String>>(){}.type)
            SplitsIOUploadRequest(claimUri, uploadUri, fields)
        }

        val runAdapter = object : TypeAdapter<Category>() {
            // According to https://github.com/glacials/splits-io/tree/master/public/schema

            private fun addToRealm(gameName: String,
                                   categoryName: String,
                                   segmentsInfo: List<Segment>): Category {

                val realm = Realm.getDefaultInstance()
                val game =  realm.getGameByName(gameName) ?: realm.addGame(gameName)
                val category = game.getCategoryByName(categoryName) ?: game.addCategory(categoryName)
                category.splits.deleteAllFromRealm()
                segmentsInfo.forEach {
                    val split = category.addSplit(it.segmentName)
                    split.updateData(pbTime = it.pbDuration, bestTime = it.bestDuration)
                }
                category.setPBFromSplits()
                realm.close()
                return category
            }

            override fun write(out: JsonWriter?, category: Category?) {
                category ?: return
                var splitTime = 0L
                out?.apply {
                    beginObject()
                    name("_schemaVersion").value("v1.0.0")
                    name("timer").beginObject()
                        name("shortname").value("fst")
                        name("longname").value("FloatingSpeedrunTimer")
                        name("version").value("v${BuildConfig.VERSION_NAME}")
                        endObject()
                    name("game").beginObject()
                        name("longname").value(category.gameName)
                        endObject()
                    name("category").beginObject()
                        name("longname").value(category.name)
                        endObject()
                    name("segments").beginArray()
                    category.splits.forEach {
                        splitTime += it.pbTime
                        beginObject()
                        name("name").value(it.name)
                        name("endedAt").beginObject()
                            name("realtimeMS").value(splitTime)
                            endObject()
                        name("bestDuration").beginObject()
                            name("realtimeMS").value(it.bestTime)
                            endObject()
                        endObject()
                    }
                    endArray()
                    endObject()
                }
            }

            override fun read(`in`: JsonReader?): Category {
                var category: Category? = null
                `in`?.apply {
                    var gameName = ""
                    var categoryName = ""
                    var segmentsInfo: List<Segment> = emptyList()
                    beginObject()
                    while (hasNext()) {
                        when (nextName()) {
                            "game" -> {
                                beginObject()
                                nextName()
                                gameName = nextString()
                                endObject()
                            }
                            "category" -> {
                                beginObject()
                                nextName()
                                categoryName = nextString()
                                endObject()
                            }
                            "segments" -> {
                                var prevSplitTime = 0L
                                beginArray()
                                while (hasNext()) {
                                    var segmentName = ""
                                    var pbDuration = 0L
                                    var bestDuration = 0L
                                    beginObject()
                                    while (hasNext()) {
                                        when (nextName()) {
                                            "name" -> segmentName = nextString()
                                            "endedAt" -> {
                                                beginObject()
                                                nextName()
                                                val endedAt = nextString().toLong()
                                                pbDuration = endedAt - prevSplitTime
                                                prevSplitTime = endedAt
                                                endObject()
                                            }
                                            "bestDuration" -> {
                                                beginObject()
                                                nextName()
                                                bestDuration = nextString().toLong()
                                                endObject()
                                            }
                                        }
                                    }
                                    endObject()
                                    segmentsInfo += Segment(segmentName, pbDuration, bestDuration)
                                }
                                endArray()
                            }
                            else -> skipValue()
                        }
                    }
                    endObject()
                    category = addToRealm(gameName, categoryName, segmentsInfo)
                }
                return category!!
            }
        }

        return GsonBuilder()
                .registerTypeAdapter(SplitsIORun::class.java, runDeserializer)
                .registerTypeAdapter(SplitsIOUploadRequest::class.java, runUploadRequestDeserializer)
                .registerTypeAdapter(Category::class.java, runAdapter)
                .excludeFieldsWithoutExposeAnnotation()
                .create()
    }

    private fun setupApi(): SplitsIOAPI {
        return Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(BASE_URL)
                .build()
                .create(SplitsIOAPI::class.java)
    }

    suspend fun getRun(id: String): SplitsIORun? {
        val runRequest = api.run(id).awaitResult()
        return (runRequest as? Result.Ok)?.value
    }

    suspend fun uploadRun(runFile: File): Boolean {
        val response1 = api.requestUploadRun().awaitResult()
        val uploadRequest = (response1 as? Result.Ok)?.value ?: return false

        val requestFile = RequestBody.create(MediaType.parse("application/octet-stream"), runFile)
        val partMap: Map<String, RequestBody> = uploadRequest.fields.mapValues {
            RequestBody.create(null, it.value)
        } + ("file" to requestFile)
        val response2 = api.uploadRun(uploadRequest.uploadUri, partMap).awaitResult()
        return response2 is Result.Ok
    }

    fun serializeRun(category: Category): String {
        val realm = Realm.getDefaultInstance()
        val json = gson.toJson(realm.copyFromRealm(category))
        realm.close()
        return json
    }

    // Also creates all necessary Realm objects
    fun deserializeRun(json: String): Category = gson.fromJson(json, Category::class.java)

    private class Segment(val segmentName: String,
                          val pbDuration: Long,
                          val bestDuration: Long)
}
