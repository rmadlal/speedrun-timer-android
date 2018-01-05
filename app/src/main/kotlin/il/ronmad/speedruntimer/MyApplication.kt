package il.ronmad.speedruntimer

import android.app.Application

import io.realm.Realm
import retrofit2.Retrofit

class MyApplication : Application() {

    lateinit var srcApi: SrcAPI
    var srcGameCache: Map<String, SrcGame?> = mapOf()
    var srcLeaderboardCache: Map<String, List<SrcLeaderboard>> = mapOf()

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        srcApi = Src.srcAPI()
    }
}
