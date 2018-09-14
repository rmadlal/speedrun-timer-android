package il.ronmad.speedruntimer.realm

import com.google.gson.annotations.Expose
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

open class Game : RealmObject(), HasPrimaryId {

    @PrimaryKey
    override var id: Long = 0L

    @Expose
    @Index
    var name: String = ""
        set(value) {
            field = value
            this.categories.forEach { it.gameName = value }
        }

    @Expose
    var categories: RealmList<Category> = RealmList()

    var timerPosition: Point? = null
}
