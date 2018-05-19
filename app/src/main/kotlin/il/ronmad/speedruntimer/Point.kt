package il.ronmad.speedruntimer

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Point : RealmObject() {

    @PrimaryKey var id: Long = 0
    var x: Int = 0
    var y: Int = 0
}
