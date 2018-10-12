package il.ronmad.speedruntimer

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import il.ronmad.speedruntimer.realm.Category
import il.ronmad.speedruntimer.realm.Game
import il.ronmad.speedruntimer.realm.Point
import il.ronmad.speedruntimer.realm.Split
import io.realm.FieldAttribute
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmSchema

const val REALM_SCHEMA_VERSION = 4L

class MyApplication : Application() {

    var installedAppsMap: Map<String, ApplicationInfo> = emptyMap()

    override fun onCreate() {
        super.onCreate()
        initRealm()
    }

    // This is slow. Should call from a background thread.
    fun setupInstalledAppsMap() {
        if (installedAppsMap.isNotEmpty())
            return
        installedAppsMap = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter {
                    it.flags and ApplicationInfo.FLAG_SYSTEM == 0 && it.packageName != packageName
                }
                .associateBy {
                    packageManager.getApplicationLabel(it).toString().toLowerCase()
                }
    }

    private fun initRealm() {
        Realm.init(this)
        var gamePrimaryKey = 0L
        var categoryPrimaryKey = 0L
        var splitPrimaryKey = 0L
        var pointPrimaryKey = 0L
        val realmConfig = RealmConfiguration.Builder()
                .schemaVersion(REALM_SCHEMA_VERSION)
                .migration { realm, oldVersion, newVersion ->
                    Log.d("MigrateRealm", "old: $oldVersion, new: $newVersion")
                    realm.schema.apply {
                        var oldVer = oldVersion.toInt()

                        fun updateVersion(updater: RealmSchema.() -> Unit) {
                            updater()
                            ++oldVer
                        }

                        if (oldVer == 0) {
                            // Split class added, RealmList<Split> field added to Category class
                            updateVersion {
                                val splitSchema = create(Split::class.java.simpleName)
                                        .addField("name", String::class.java, FieldAttribute.REQUIRED)
                                        .addField("pbTime", Long::class.java, FieldAttribute.REQUIRED)
                                        .addField("bestTime", Long::class.java, FieldAttribute.REQUIRED)
                                get(Category::class.java.simpleName)
                                        ?.addRealmListField("splits", splitSchema)
                            }
                        }
                        if (oldVer == 1) {
                            // "name" fields have been made indexed
                            updateVersion {
                                get(Game::class.java.simpleName)?.addIndex("name")
                                get(Category::class.java.simpleName)?.addIndex("name")
                                get(Split::class.java.simpleName)?.addIndex("name")
                            }
                        }
                        if (oldVer == 2) {
                            // added primary keys to all objects (id: Long)
                            updateVersion {
                                get(Game::class.java.simpleName)
                                        ?.addField("id", Long::class.java, FieldAttribute.INDEXED)
                                        ?.transform { it.setLong("id", ++gamePrimaryKey) }
                                        ?.addPrimaryKey("id")
                                get(Category::class.java.simpleName)
                                        ?.addField("id", Long::class.java, FieldAttribute.INDEXED)
                                        ?.transform { it.setLong("id", ++categoryPrimaryKey) }
                                        ?.addPrimaryKey("id")
                                get(Split::class.java.simpleName)
                                        ?.addField("id", Long::class.java, FieldAttribute.INDEXED)
                                        ?.transform { it.setLong("id", ++splitPrimaryKey) }
                                        ?.addPrimaryKey("id")
                                get(Point::class.java.simpleName)
                                        ?.addField("id", Long::class.java, FieldAttribute.INDEXED)
                                        ?.transform { it.setLong("id", ++pointPrimaryKey) }
                                        ?.addPrimaryKey("id")
                            }
                        }
                        if (oldVer == 3) {
                            // gameName field added to Category class
                            updateVersion {
                                get(Category::class.java.simpleName)
                                        ?.addField("gameName", String::class.java, FieldAttribute.REQUIRED)
                                        ?.transform {
                                            it.linkingObjects(Game::class.java.simpleName, "categories")
                                                    // Only one game has a reference to this category.
                                                    .singleOrNull()?.let { game ->
                                                        it.setString("gameName", game.getString("name"))
                                                    }
                                        }
                            }
                        }
                    }
                }
                .build()
        Realm.setDefaultConfiguration(realmConfig)
    }
}
