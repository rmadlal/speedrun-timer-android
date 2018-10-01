package il.ronmad.speedruntimer.web

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import il.ronmad.speedruntimer.BuildConfig
import il.ronmad.speedruntimer.readSingleObjectValue
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResponse
import ru.gildor.coroutines.retrofit.awaitResult

const val BASE_URL = "https://splits.io/api/v4/"

interface SplitsIOAPI {
    @Headers("Accept: application/splitsio")    // https://github.com/glacials/splits-io/tree/master/public/schema
    @GET("runs/{id}")
    fun getRun(@Path("id") id: String): Call<SplitsIO.Run>

    @POST("runs")
    fun requestUploadRun(): Call<SplitsIO.UploadRequest>

    @Multipart
    @POST
    fun uploadRun(@Url claimUri: String,
                  @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>): Call<ResponseBody>
}

class SplitsIO {

    private val gson = setupGson()
    private val api = setupApi()

    private fun setupGson(): Gson {
        val runUploadRequestAdapter = object : TypeAdapter<UploadRequest>() {
            // https://github.com/glacials/splits-io/blob/master/docs/api.md#uploading

            override fun write(out: JsonWriter?, value: UploadRequest?) { /* Irrelevant */ }

            override fun read(`in`: JsonReader?): UploadRequest {
                return `in`!!.run {
                    var claimUri = ""
                    var uploadUri = ""
                    var fields: Map<String, String> = emptyMap()
                    beginObject()
                    while (hasNext()) {
                        when (nextName()) {
                            "uris" -> claimUri = readSingleObjectValue("claim_uri")
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
                    UploadRequest(claimUri, uploadUri, fields)
                }
            }
        }

        val runAdapter = object : TypeAdapter<Run>() {

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
                        run.segments.forEach {
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

            override fun read(`in`: JsonReader?): Run {
                return `in`!!.run {
                    var gameName = ""
                    var categoryName = ""
                    var segments: List<Segment> = emptyList()
                    var attemptsTotal = 0
                    beginObject()
                    while (hasNext()) {
                        when (nextName()) {
                            "attempts" -> attemptsTotal = readSingleObjectValue("total").toInt()
                            "game" -> gameName = readSingleObjectValue("longname")
                            "category" -> categoryName = readSingleObjectValue("longname")
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
                                                val endedAt = readSingleObjectValue("realtimeMS").toLong()
                                                pbDuration = endedAt - prevSplitTime
                                                prevSplitTime = endedAt
                                            }
                                            "bestDuration" -> bestDuration = readSingleObjectValue("realtimeMS").toLong()
                                            else -> skipValue()
                                        }
                                    }
                                    endObject()
                                    segments += Segment(segmentName, pbDuration, bestDuration)
                                }
                                endArray()
                            }
                            else -> skipValue()
                        }
                    }
                    endObject()
                    Run(gameName, categoryName, attemptsTotal, segments)
                }
            }
        }

        return GsonBuilder()
                .registerTypeAdapter(UploadRequest::class.java, runUploadRequestAdapter)
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
        val runRequest = api.getRun(id).awaitResult()
        return (runRequest as? Result.Ok)?.value
    }

    /**
     * https://github.com/glacials/splits-io/blob/master/docs/api.md#uploading
     * @return Claim URI for the uploaded run, or empty String if failed.
     */
    suspend fun uploadRun(run: Run): String? {
        val response1 = api.requestUploadRun().awaitResult()
        return (response1 as? Result.Ok)?.value?.let { uploadRequest ->
            val requestFile = RequestBody.create(
                    MediaType.parse("application/octet-stream"),
                    serializeRun(run))
            val partMap: Map<String, RequestBody> = uploadRequest.fields.mapValues {
                RequestBody.create(null, it.value)
            } + ("file" to requestFile)
            val response2 = api.uploadRun(uploadRequest.uploadUri, partMap).awaitResponse()
            if (response2.isSuccessful) uploadRequest.claimUri else null
        }
    }

    fun serializeRun(run: Run): String = gson.toJson(run)

    fun deserializeRun(json: String): Run = gson.fromJson(json, Run::class.java)

    class Run(val gameName: String,
              val categoryName: String,
              val attemptsTotal: Int,
              val segments: List<Segment>)

    class Segment(val segmentName: String,
                  val pbDuration: Long,
                  val bestDuration: Long)

    class UploadRequest(val claimUri: String,
                        val uploadUri: String,
                        val fields: Map<String, String>)
}
