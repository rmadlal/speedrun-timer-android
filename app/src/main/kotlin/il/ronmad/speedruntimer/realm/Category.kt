package il.ronmad.speedruntimer.realm

import com.google.gson.annotations.Expose
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

open class Category : RealmObject(), HasPrimaryId {

    @PrimaryKey
    override var id: Long = 0L

    @Expose
    @Index
    var name: String = ""

    @Expose
    var gameName: String = ""

    var bestTime: Long = 0L

    @Expose
    var runCount: Int = 0

    @Expose
    var splits: RealmList<Split> = RealmList()

    @LinkingObjects("categories")
    val game: RealmResults<Game>? = null

    fun updateSplits(segmentTimes: List<Long>, isNewPB: Boolean) {
        splits.forEachIndexed { index, split ->
            split.update(segmentTimes.getOrElse(index) { 0L }, isNewPB)
        }
    }
}
