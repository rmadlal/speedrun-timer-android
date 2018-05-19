package il.ronmad.speedruntimer

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

open class Category : RealmObject() {

    @PrimaryKey var id: Long = 0
    @Index var name: String = ""
    var bestTime: Long = 0
    var runCount: Int = 0
    var splits: RealmList<Split> = RealmList()

    @LinkingObjects("categories")
    val game: RealmResults<Game>? = null

    fun updateSplits(segmentTimes: List<Long>, isNewPB: Boolean) {
        splits.forEachIndexed { index, split ->
            split.update(segmentTimes.getOrElse(index) { 0L }, isNewPB) }
    }
}
