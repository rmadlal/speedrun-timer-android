package il.ronmad.speedruntimer

import android.content.Context
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.view.View
import kotlinx.android.synthetic.main.timer_overlay.view.*

import java.lang.ref.WeakReference

class Chronometer(val context: Context, val view: View) : TimeExtensions {

    private var base = 0L

    internal var timeElapsed: Long = 0
        private set

    private var timerColor = colorNeutral
        set(value) {
            if (value == field) return
            setColors(value)
            field = value
        }

    internal var compareAgainst = 0L
    private val chronoHandler: Handler

    init {
        if (!showMillis) {
            view.chronoMilli1.visibility = View.GONE
            view.chronoMilli2.visibility = View.GONE
            view.dot.visibility = View.GONE
        }
        setColors(colorNeutral)
        chronoHandler = ChronoHandler(this)
        init()
    }

    private fun init() {
        started = false
        timeElapsed = -countdown
        compareAgainst = 0L
        updateTime()
        updateVisibility()
        timerColor = colorNeutral
    }

    internal fun start() {
        started = true
        running = true
        base = SystemClock.elapsedRealtime() - timeElapsed
        updateRunning()
    }

    internal fun stop() {
        running = false
        updateRunning()
        if (timeElapsed > 0 && (compareAgainst == 0L || timeElapsed < compareAgainst)) {
            timerColor = colorPB
        }
    }

    internal fun reset() {
        stop()
        init()
    }

    private fun update() {
        timeElapsed = SystemClock.elapsedRealtime() - base
        updateTime()
        updateVisibility()
        updateColor()
    }

    private fun updateTime() {
        val (hours, minutes, seconds, millis) = timeElapsed.getTimeUnits(true)
        view.chronoHr2.text = (hours / 10).toString()
        view.chronoHr1.text = (hours % 10).toString()
        view.chronoMin2.text = (minutes / 10).toString()
        view.chronoMin1.text = (minutes % 10).toString()
        view.chronoSec2.text = (seconds / 10).toString()
        view.chronoSec1.text = (seconds % 10).toString()
        view.chronoMilli2.text = (millis / 10).toString()
        view.chronoMilli1.text = (millis % 10).toString()
    }

    private fun updateVisibility() {
        val (hours, minutes, seconds, _) = timeElapsed.getTimeUnits(true)
        view.chronoMinus.visibility = if (timeElapsed < 0) View.VISIBLE else View.GONE
        when {
            hours > 0 -> {
                view.chronoHr2.visibility = if (hours / 10 > 0) View.VISIBLE else View.GONE
                view.chronoHr1.visibility = View.VISIBLE
                view.hrMinColon.visibility = View.VISIBLE
                view.chronoMin2.visibility = View.VISIBLE
                view.chronoMin1.visibility = View.VISIBLE
                view.minSecColon.visibility = View.VISIBLE
                view.chronoSec2.visibility = View.VISIBLE
            }
            minutes > 0 -> {
                view.chronoHr2.visibility = View.GONE
                view.chronoHr1.visibility = View.GONE
                view.hrMinColon.visibility = View.GONE
                view.chronoMin2.visibility = if (minutes / 10 > 0) View.VISIBLE else View.GONE
                view.chronoMin1.visibility = View.VISIBLE
                view.minSecColon.visibility = View.VISIBLE
                view.chronoSec2.visibility = View.VISIBLE
            }
            else -> {
                view.chronoHr2.visibility = View.GONE
                view.chronoHr1.visibility = View.GONE
                view.hrMinColon.visibility = View.GONE
                view.chronoMin2.visibility = View.GONE
                view.chronoMin1.visibility = if (alwaysMinutes) View.VISIBLE else View.GONE
                view.minSecColon.visibility = if (alwaysMinutes) View.VISIBLE else View.GONE
                view.chronoSec2.visibility = if (alwaysMinutes || seconds / 10 > 0) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateColor() {
        if (compareAgainst == 0L || timeElapsed < 0) {
            timerColor = colorNeutral
            return
        }
        timerColor = if (timeElapsed < compareAgainst) colorAhead else colorBehind
    }

    private fun updateRunning() {
        if (running) {
            update()
            chronoHandler.sendMessageDelayed(Message.obtain(chronoHandler, TICK_WHAT), 15)
        } else {
            chronoHandler.removeMessages(TICK_WHAT)
        }
    }

    private fun setColors(color: Int) {
        view.chronoHr2.setTextColor(color)
        view.chronoHr1.setTextColor(color)
        view.hrMinColon.setTextColor(color)
        view.chronoMin2.setTextColor(color)
        view.chronoMin1.setTextColor(color)
        view.minSecColon.setTextColor(color)
        view.chronoSec2.setTextColor(color)
        view.chronoSec1.setTextColor(color)
        view.dot.setTextColor(color)
        view.chronoMilli2.setTextColor(color)
        view.chronoMilli1.setTextColor(color)
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
        var alwaysMinutes = false
        var started = false
        var running = false
        const val TICK_WHAT = 2
    }
}
