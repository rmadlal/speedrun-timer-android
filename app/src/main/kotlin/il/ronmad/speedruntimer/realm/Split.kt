package il.ronmad.speedruntimer.realm

import com.google.gson.annotations.Expose
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

open class Split : RealmObject(), HasPrimaryId {

    @PrimaryKey
    override var id: Long = 0L

    @Expose
    @Index
    var name: String = ""

    @Expose
    var pbTime: Long = 0L

    @Expose
    var bestTime: Long = 0L
        set(value) {
            field = if (value != 0L) value.coerceAtMost(pbTime) else pbTime
        }

    @LinkingObjects("splits")
    val category: RealmResults<Category>? = null

    fun update(segmentTime: Long, isNewPB: Boolean) {
        if (segmentTime == 0L) return
        if (isNewPB) {
            updateData(pbTime = segmentTime)
        }
        if (bestTime == 0L || segmentTime < bestTime) {
            updateData(bestTime = segmentTime)
        }
    }
}
