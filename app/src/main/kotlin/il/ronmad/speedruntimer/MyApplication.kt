package il.ronmad.speedruntimer

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import io.realm.FieldAttribute

import io.realm.Realm
import io.realm.RealmConfiguration

class MyApplication : Application() {

    lateinit var srcApi: SrcAPI
    var srcGameCache: Map<String, SrcGame> = emptyMap()

    private var installedApps: Map<String, ApplicationInfo> = mapOf()
    var installedGames: List<String> = listOf()

    override fun onCreate() {
        super.onCreate()
        initRealm()
        srcApi = Src.srcAPI()
        setupInstalledAppsLists()
    }

    fun tryLaunchGame(gameName: String): Boolean {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (!sharedPrefs.getBoolean(getString(R.string.key_pref_launch_games), true)) {
            return false
        }
        installedApps[gameName.toLowerCase()]?.let {
            showToast("Launching ${packageManager.getApplicationLabel(it)}...")
            startActivity(packageManager.getLaunchIntentForPackage(it.packageName))
            return true
        }
        return false
    }

    private fun initRealm() {
        Realm.init(this)
        var gamePrimaryKey = 0L
        var categoryPrimaryKey = 0L
        var splitPrimaryKey = 0L
        var pointPrimaryKey = 0L
        val realmConfig = RealmConfiguration.Builder()
                .schemaVersion(3)
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
                    if (oldVer == 2) {
                        // added primary keys to all objects (id: Long)
                        realm.schema.get("Game")
                                ?.addField("id", Long::class.java, FieldAttribute.INDEXED)
                                ?.transform { it.setLong("id", ++gamePrimaryKey) }
                                ?.addPrimaryKey("id")
                        realm.schema.get("Category")
                                ?.addField("id", Long::class.java, FieldAttribute.INDEXED)
                                ?.transform { it.setLong("id", ++categoryPrimaryKey) }
                                ?.addPrimaryKey("id")
                        realm.schema.get("Split")
                                ?.addField("id", Long::class.java, FieldAttribute.INDEXED)
                                ?.transform { it.setLong("id", ++splitPrimaryKey) }
                                ?.addPrimaryKey("id")
                        realm.schema.get("Point")
                                ?.addField("id", Long::class.java, FieldAttribute.INDEXED)
                                ?.transform { it.setLong("id", ++pointPrimaryKey) }
                                ?.addPrimaryKey("id")
                        ++oldVer
                    }
                }
                .build()
        Realm.setDefaultConfiguration(realmConfig)
    }

    private fun setupInstalledAppsLists() {
        val allInstalledApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter {
                    it.flags and ApplicationInfo.FLAG_SYSTEM == 0 && it.packageName != packageName
                }
        installedApps = allInstalledApps
                .map { packageManager.getApplicationLabel(it).toString().toLowerCase() to it }
                .toMap()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            installedGames = allInstalledApps
                    .filter { it.category == ApplicationInfo.CATEGORY_GAME }
                    .map { packageManager.getApplicationLabel(it).toString() }
        }
    }
}
