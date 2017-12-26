package il.ronmad.speedruntimer

import io.realm.RealmList
import io.realm.RealmObject

open class Game : RealmObject() {

    var name: String = ""
    var categories: RealmList<Category> = RealmList()
    var timerPosition: Point? = null
}
