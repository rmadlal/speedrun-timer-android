package il.ronmad.speedruntimer

import android.app.Application

import io.realm.Realm
import retrofit2.Retrofit

class MyApplication : Application() {

    lateinit var srcApi: SrcAPI

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        srcApi = Retrofit.Builder()
                .addConverterFactory(Src.gsonConverter())
                .baseUrl(SRC_API)
                .build()
                .create(SrcAPI::class.java)
    }
}
