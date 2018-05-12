package il.ronmad.speedruntimer

import io.realm.RealmList
import io.realm.RealmObject

open class Category : RealmObject() {

    var name: String = ""
    var bestTime: Long = 0
    var runCount: Int = 0
    var splits: RealmList<Split> = RealmList()

    fun updateSplits(splitTimes: List<Long>, isNewPB: Boolean) {
        splits.forEachIndexed { index, split ->
            split.update(splitTimes.getOrElse(index) { 0L }, isNewPB) }
    }
}
