package il.ronmad.speedruntimer

import android.content.Context
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.view.View
import kotlinx.android.synthetic.main.timer_overlay.view.*

import java.lang.ref.WeakReference

class Chronometer(val context: Context, val view: View) {

    private var base = 0L

    internal var timeElapsed: Long = 0
        private set

    private var compareAgainst = 0L
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
        compareAgainst = 0L
        setChronoTextFromTime(timeElapsed)
        setColor(colorNeutral)
    }

    internal fun start(nextSegmentSplitTime: Long) {
        split(nextSegmentSplitTime)
        if (compareAgainst == 0L) {
            setColor(colorNeutral)
        }
        started = true
        running = true
        base = SystemClock.elapsedRealtime() - timeElapsed
        updateRunning()
    }

    internal fun split(nextSegmentSplitTime: Long) {
        compareAgainst = nextSegmentSplitTime
    }

    internal fun stop() {
        running = false
        updateRunning()
        if (timeElapsed > 0 && (compareAgainst == 0L || timeElapsed < compareAgainst)) {
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
        val units = time.getTimeUnits(true)
        val (_, _, _, millis) = units

        view.chronoRest.text = time.getFormattedTime(withMillis = false, forceMinutes = true)
        view.chronoMillis.text = ".%02d".format(millis)
    }

    private fun updateColor() {
        if (compareAgainst == 0L || timeElapsed < 0) {
            return
        }
        if (timeElapsed < compareAgainst && view.chronoRest.currentTextColor != colorAhead) {
            setColor(colorAhead)
        } else if (timeElapsed >= compareAgainst && view.chronoRest.currentTextColor != colorBehind) {
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

        var colorNeutral = 0
        var colorAhead = 0
        var colorBehind = 0
        var colorPB = 0
        var colorBestSegment = 0
        var countdown = 0L
        var showMillis = false
        var started = false
        var running = false
        const val TICK_WHAT = 2
    }
}
