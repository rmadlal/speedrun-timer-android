package il.ronmad.speedruntimer

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

open class Game : RealmObject() {

    @PrimaryKey var id: Long = 0
    @Index var name: String = ""
    var categories: RealmList<Category> = RealmList()
    var timerPosition: Point? = null
}
