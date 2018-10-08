package il.ronmad.speedruntimer.ui.util

class Event<out T>(private val data: T) {

    var wasHandled = false
        private set

    fun handle(): T? {
        return if (wasHandled) null
        else {
            wasHandled = true
            data
        }
    }

    fun release() {
        wasHandled = false
    }
}
