package il.ronmad.speedruntimer

import android.content.Context
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.view.View
import kotlinx.android.synthetic.main.timer_overlay.view.*

import java.lang.ref.WeakReference
import java.util.Locale

class Chronometer(val context: Context, val view: View) {

    private var base: Long = 0

    internal var timeElapsed: Long = 0
        private set

    private val chronoHandler: Handler

    init {
        if (!showMillis) {
            view.chronoMillis.visibility = View.GONE
        }
        chronoHandler = ChronoHandler(this)
        init()
    }

    private fun init() {
        started = false
        timeElapsed = -1 * countdown
        setChronoTextFromTime(timeElapsed)
        setColor(colorNeutral)
    }

    internal fun start() {
        started = true
        running = true
        base = SystemClock.elapsedRealtime() - timeElapsed
        updateRunning()
        if (bestTime == 0L) {
            setColor(colorNeutral)
        }
    }

    internal fun stop() {
        running = false
        updateRunning()
        if (timeElapsed > 0 && (bestTime == 0L || timeElapsed < bestTime)) {
            setColor(colorPB)
        }
    }

    internal fun reset() {
        stop()
        init()
    }

    private fun update() {
        timeElapsed = SystemClock.elapsedRealtime() - base
        setChronoTextFromTime(timeElapsed)
        updateColor()
    }

    private fun setChronoTextFromTime(time: Long) {
        val units = Math.abs(time).getTimeUnits()
        val hours = units[0]
        val minutes = units[1]
        val seconds = units[2]
        val millis = units[3] / 10
        view.chronoRest.text = if (hours > 0)
                String.format(Locale.getDefault(),
                        if (time < 0) "-%d:%02d:%02d" else "%d:%02d:%02d", hours, minutes, seconds)
            else String.format(Locale.getDefault(),
                        if (time < 0) "-%d:%02d" else "%d:%02d", minutes, seconds)
        view.chronoMillis.text = String.format(Locale.getDefault(), ".%02d", millis)
    }

    private fun updateColor() {
        if (bestTime == 0L || timeElapsed < 0) {
            return
        }
        if (timeElapsed < bestTime && view.chronoRest.currentTextColor != colorAhead) {
            setColor(colorAhead)
        } else if (timeElapsed >= bestTime && view.chronoRest.currentTextColor != colorBehind) {
            setColor(colorBehind)
        }
    }

    private fun updateRunning() {
        if (running) {
            update()
            chronoHandler.sendMessageDelayed(Message.obtain(chronoHandler, TICK_WHAT), 15)
        } else {
            chronoHandler.removeMessages(TICK_WHAT)
        }
    }

    private fun setColor(color: Int) {
        view.chronoRest.setTextColor(color)
        view.chronoMillis.setTextColor(color)
    }

    private class ChronoHandler internal constructor(instance: Chronometer) : Handler() {

        private val instance: WeakReference<Chronometer> = WeakReference(instance)

        override fun handleMessage(m: Message) {
            val mChronometer = instance.get()
            mChronometer?.let {
                if (running) {
                    it.update()
                    sendMessageDelayed(Message.obtain(this, TICK_WHAT), 15)
                }
            }
        }
    }

    companion object {

        internal var bestTime: Long = 0
        internal var colorNeutral: Int = 0
        internal var colorAhead: Int = 0
        internal var colorBehind: Int = 0
        internal var colorPB: Int = 0
        internal var countdown: Long = 0
        internal var showMillis: Boolean = false
        internal var started: Boolean = false
        internal var running: Boolean = false
        private val TICK_WHAT = 2
    }
}
