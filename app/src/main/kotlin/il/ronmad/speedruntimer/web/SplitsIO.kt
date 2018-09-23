package il.ronmad.speedruntimer.web

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import il.ronmad.speedruntimer.BuildConfig
import il.ronmad.speedruntimer.realm.Category
import il.ronmad.speedruntimer.realm.toRun
import il.ronmad.speedruntimer.toRealmCategory
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
    @Headers("Accept: application/json")    // awaiting support for application/splitsio
    @GET("runs/{id}")
    fun run(@Path("id") id: String): Call<SplitsIO.Run>

    @POST("runs")
    fun requestUploadRun(): Call<SplitsIOUploadRequest>


    @Multipart
    @POST
    fun uploadRun(@Url claimUri: String,
                  @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>): Call<ResponseBody>
}

/*
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
*/

data class SplitsIOUploadRequest(val claimUri: String,
                                 val uploadUri: String,
                                 val fields: Map<String, String>)

class SplitsIO {

    private val gson = setupGson()
    private val api = setupApi()

    private fun setupGson(): Gson {

/*
        val splitsIORunDeserializer = JsonDeserializer<SplitsIORun> { json, _, _ ->
            val run = json.asJsonObject.get("run")
            Gson().fromJson(run, SplitsIORun::class.java)
        }
*/

        val runUploadRequestAdapter = object : TypeAdapter<SplitsIOUploadRequest>() {
            // According to https://github.com/glacials/splits-io/blob/master/docs/api.md

            override fun write(out: JsonWriter?, value: SplitsIOUploadRequest?) { /* Irrelevant */ }

            override fun read(`in`: JsonReader?): SplitsIOUploadRequest {
                return `in`!!.run {
                    var claimUri = ""
                    var uploadUri = ""
                    var fields: Map<String, String> = emptyMap()
                    beginObject()
                    while (hasNext()) {
                        when (nextName()) {
                            "uris" -> {
                                beginObject()
                                while (hasNext()) {
                                    when (nextName()) {
                                        "claim_uri" -> claimUri = nextString()
                                        else -> skipValue()
                                    }
                                }
                                endObject()
                            }
                            "presigned_request" -> {
                                beginObject()
                                while (hasNext()) {
                                    when (nextName()) {
                                        "uri" -> uploadUri = nextString()
                                        "fields" -> {
                                            beginObject()
                                            while (hasNext()) {
                                                val key = nextName()
                                                fields += key to nextString()
                                            }
                                            endObject()
                                        }
                                        else -> skipValue()
                                    }
                                }
                                endObject()
                            }
                            else -> skipValue()
                        }
                    }
                    endObject()
                    SplitsIOUploadRequest(claimUri, uploadUri, fields)
                }
            }
        }

        val runAdapter = object : TypeAdapter<Run>() {
            // According to https://github.com/glacials/splits-io/tree/master/public/schema

            // application/splitsio
            override fun write(out: JsonWriter?, run: Run?) {
                run ?: return
                out?.run {
                    beginObject()
                    name("_schemaVersion").value("v1.0.0")
                    name("timer").beginObject()
                        name("shortname").value("fst")
                        name("longname").value("Floating Speedrun Timer")
                        name("version").value("v${BuildConfig.VERSION_NAME}")
                        endObject()
                    name("attempts").beginObject()
                        name("total").value(run.attemptsTotal)
                        endObject()
                    name("game").beginObject()
                        name("longname").value(run.gameName)
                        endObject()
                    name("category").beginObject()
                        name("longname").value(run.categoryName)
                        endObject()
                    name("segments").beginArray()
                    var splitTime = 0L
                    run.segmentsInfo.forEach {
                        splitTime += it.pbDuration
                        beginObject()
                        name("name").value(it.segmentName)
                        name("endedAt").beginObject()
                            name("realtimeMS").value(splitTime)
                            endObject()
                        name("bestDuration").beginObject()
                            name("realtimeMS").value(it.bestDuration)
                            endObject()
                        endObject()
                    }
                    endArray()
                    endObject()
                }
            }

            // application/json
            override fun read(`in`: JsonReader?): Run {
                return `in`!!.run {
                    beginObject()
                    nextName()  // "run"
                    var gameName = ""
                    var categoryName = ""
                    var segmentsInfo: List<Segment> = emptyList()
                    var attemptsTotal = 0
                    beginObject()
                    while (hasNext()) {
                        when (nextName()) {
                            "attempts" -> attemptsTotal = nextInt()
                            "game" -> {
                                beginObject()
                                while (hasNext()) {
                                    when (nextName()) {
                                        "name" -> gameName = nextString()
                                        else -> skipValue()
                                    }
                                }
                                endObject()
                            }
                            "category" -> {
                                beginObject()
                                while (hasNext()) {
                                    when (nextName()) {
                                        "name" -> categoryName = nextString()
                                        else -> skipValue()
                                    }
                                }
                                endObject()
                            }
                            "segments" -> {
                                beginArray()
                                while (hasNext()) {
                                    var segmentName = ""
                                    var pbDuration = 0L
                                    var bestDuration = 0L
                                    beginObject()
                                    while (hasNext()) {
                                        when (nextName()) {
                                            "name" -> segmentName = nextString()
                                            "realtime_duration_ms" -> pbDuration = nextString().toLong()
                                            "realtime_shortest_duration_ms" -> bestDuration = nextString().toLong()
                                            else -> skipValue()
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
                    endObject()
                    Run(gameName, categoryName, attemptsTotal, segmentsInfo)
                }
            }

/*
            // application/splitsio
            override fun read(`in`: JsonReader?): Run {
                return `in`!!.run {
                    var gameName = ""
                    var categoryName = ""
                    var segmentsInfo: List<Segment> = emptyList()
                    var attemptsTotal = 0
                    beginObject()
                    while (hasNext()) {
                        when (nextName()) {
                            "attempts" -> {
                                beginObject()
                                while (hasNext()) {
                                    when(nextName()) {
                                        "total" -> attemptsTotal = nextInt()
                                        else -> skipValue()
                                    }
                                }
                                endObject()
                            }
                            "game" -> {
                                beginObject()
                                nextName()  // "longname"
                                gameName = nextString()
                                endObject()
                            }
                            "category" -> {
                                beginObject()
                                nextName()  // "longname"
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
                                                nextName()  // "realtimeMS"
                                                val endedAt = nextString().toLong()
                                                pbDuration = endedAt - prevSplitTime
                                                prevSplitTime = endedAt
                                                endObject()
                                            }
                                            "bestDuration" -> {
                                                beginObject()
                                                nextName()  // "realtimeMS"
                                                bestDuration = nextString().toLong()
                                                endObject()
                                            }
                                            else -> skipValue()
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
                    Run(gameName, categoryName, attemptsTotal, segmentsInfo)
                }
            }
*/

        }

        return GsonBuilder()
//                .registerTypeAdapter(SplitsIORun::class.java, splitsIORunDeserializer)
                .registerTypeAdapter(SplitsIOUploadRequest::class.java, runUploadRequestAdapter)
                .registerTypeAdapter(Run::class.java, runAdapter)
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

    suspend fun getRun(id: String): Run? {
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

    fun serializeRun(category: Category): String = gson.toJson(category.toRun())

    // Also creates all necessary Realm objects
    fun deserializeRun(json: String): Category =
            gson.fromJson(json, Run::class.java).toRealmCategory()

    class Run(val gameName: String,
              val categoryName: String,
              val attemptsTotal: Int,
              val segmentsInfo: List<Segment>)

    class Segment(val segmentName: String,
                  val pbDuration: Long,
                  val bestDuration: Long)
}
