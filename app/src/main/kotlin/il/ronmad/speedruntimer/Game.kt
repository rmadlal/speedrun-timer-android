package il.ronmad.speedruntimer

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index

open class Game : RealmObject() {

    @Index var name: String = ""
    var categories: RealmList<Category> = RealmList()
    var timerPosition: Point? = null
}
