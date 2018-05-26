package il.ronmad.speedruntimer

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Point : RealmObject(), HasPrimaryId {

    @PrimaryKey
    override var id: Long = 0L

    var x: Int = 0
    var y: Int = 0

    operator fun component1() = this.x

    operator fun component2() = this.y
}
