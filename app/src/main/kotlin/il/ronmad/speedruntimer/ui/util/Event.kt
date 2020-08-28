package il.ronmad.speedruntimer.ui.util

class Event<out T>(private val data: T) {

    private var wasHandled = false

    fun handle(): T? {
        return if (wasHandled) null
        else {
            wasHandled = true
            data
        }
    }

    // TODO: unused
    fun release() {
        wasHandled = false
    }
}
