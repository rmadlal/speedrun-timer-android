package il.ronmad.speedruntimer

import io.realm.RealmObject

open class Category : RealmObject() {

    var name: String = ""
    var bestTime: Long = 0
    var runCount: Int = 0
}
