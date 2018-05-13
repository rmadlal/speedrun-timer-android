package il.ronmad.speedruntimer

import android.app.Application
import android.util.Log
import io.realm.FieldAttribute

import io.realm.Realm
import io.realm.RealmConfiguration

class MyApplication : Application() {

    lateinit var srcApi: SrcAPI
    var srcGameCache: Map<String, SrcGame?> = mapOf()
    var srcLeaderboardCache: Map<String, List<SrcLeaderboard>> = mapOf()

    override fun onCreate() {
        super.onCreate()
        initRealm()
        srcApi = Src.srcAPI()
    }

    private fun initRealm() {
        Realm.init(this)
        val realmConfig = RealmConfiguration.Builder()
                .schemaVersion(2)
                .migration { realm, oldVersion, newVersion ->
                    Log.d("MigrateRealm", "old: $oldVersion, new: $newVersion")
                    var oldVer = oldVersion.toInt()
                    if (oldVer == 0) {
                        // Split class added, RealmList<Split> field added to Category class
                        val splitSchema = realm.schema.create("Split")
                                .addField("name", String::class.java, FieldAttribute.REQUIRED)
                                .addField("pbTime", Long::class.java, FieldAttribute.REQUIRED)
                                .addField("bestTime", Long::class.java, FieldAttribute.REQUIRED)
                        realm.schema.get("Category")?.addRealmListField("splits", splitSchema)
                        ++oldVer
                    }
                    if (oldVer == 1) {
                        // "name" fields have been made indexed
                        realm.schema.get("Game")?.addIndex("name")
                        realm.schema.get("Category")?.addIndex("name")
                        realm.schema.get("Split")?.addIndex("name")
                        ++oldVer
                    }
                }
                .build()
        Realm.setDefaultConfiguration(realmConfig)
    }
}
