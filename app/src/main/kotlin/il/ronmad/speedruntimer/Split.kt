package il.ronmad.speedruntimer

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

open class Split : RealmObject() {

    @PrimaryKey var id: Long = 0
    @Index var name: String = ""
    var pbTime: Long = 0
    var bestTime: Long = 0

    @LinkingObjects("splits")
    val category: RealmResults<Category>? = null

    fun update(segmentTime: Long, isNewPB: Boolean) {
        if (segmentTime == 0L) return
        if (isNewPB && (pbTime == 0L || segmentTime < pbTime)) {
            updateData(pbTime = segmentTime)
        }
        if (bestTime == 0L || segmentTime < bestTime) {
            updateData(bestTime = segmentTime)
        }
    }
}
