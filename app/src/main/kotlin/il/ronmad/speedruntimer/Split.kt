package il.ronmad.speedruntimer

import io.realm.RealmObject

open class Split : RealmObject() {

    var name: String = ""
    var pbTime: Long = 0
    var bestTime: Long = 0

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