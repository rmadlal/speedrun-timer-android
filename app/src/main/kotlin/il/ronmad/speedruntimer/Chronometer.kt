package il.ronmad.speedruntimer

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.view.View
import kotlinx.android.synthetic.main.timer_overlay.view.*
import java.lang.ref.WeakReference

class Chronometer(private val chronoView: View) {

    private var base = 0L

    internal var timeElapsed: Long = 0
        private set

    private var timerColor = colorNeutral
        set(value) {
            if (value == field) return
            chronoView.chronoViewSet.forEach { it.setTextColor(value) }
            field = value
        }

    internal var compareAgainst = 0L
    private val chronoHandler: Handler

    init {
        if (!showMillis) {
            chronoView.apply {
                chronoMilli1.visibility = View.GONE
                chronoMilli2.visibility = View.GONE
                chronoDot.visibility = View.GONE
            }
        }
        chronoView.chronoViewSet.forEach { it.setTextColor(colorNeutral) }
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
        chronoView.apply {
            chronoHr2.text = (hours / 10).toString()
            chronoHr1.text = (hours % 10).toString()
            chronoMin2.text = (minutes / 10).toString()
            chronoMin1.text = (minutes % 10).toString()
            chronoSec2.text = (seconds / 10).toString()
            chronoSec1.text = (seconds % 10).toString()
            chronoMilli2.text = (millis / 10).toString()
            chronoMilli1.text = (millis % 10).toString()
        }
    }

    private fun updateVisibility() {
        val (hours, minutes, seconds, _) = timeElapsed.getTimeUnits(true)
        chronoView.apply {
            chronoMinus.visibility = if (timeElapsed < 0) View.VISIBLE else View.GONE
            when {
                hours > 0 -> {
                    chronoHr2.visibility = if (hours / 10 > 0) View.VISIBLE else View.GONE
                    chronoHr1.visibility = View.VISIBLE
                    chronoHrMinColon.visibility = View.VISIBLE
                    chronoMin2.visibility = View.VISIBLE
                    chronoMin1.visibility = View.VISIBLE
                    chronoMinSecColon.visibility = View.VISIBLE
                    chronoSec2.visibility = View.VISIBLE
                }
                minutes > 0 -> {
                    chronoHr2.visibility = View.GONE
                    chronoHr1.visibility = View.GONE
                    chronoHrMinColon.visibility = View.GONE
                    chronoMin2.visibility = if (minutes / 10 > 0) View.VISIBLE else View.GONE
                    chronoMin1.visibility = View.VISIBLE
                    chronoMinSecColon.visibility = View.VISIBLE
                    chronoSec2.visibility = View.VISIBLE
                }
                else -> {
                    chronoHr2.visibility = View.GONE
                    chronoHr1.visibility = View.GONE
                    chronoHrMinColon.visibility = View.GONE
                    chronoMin2.visibility = View.GONE
                    chronoMin1.visibility = if (alwaysMinutes) View.VISIBLE else View.GONE
                    chronoMinSecColon.visibility = if (alwaysMinutes) View.VISIBLE else View.GONE
                    chronoSec2.visibility = if (alwaysMinutes || seconds / 10 > 0) View.VISIBLE else View.GONE
                }
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

    private class ChronoHandler(instance: Chronometer) :
            Handler(Looper.myLooper() ?: Looper.getMainLooper()) {

        private val instance: WeakReference<Chronometer> = WeakReference(instance)

        override fun handleMessage(m: Message) {
            instance.get()?.let { chronometer ->
                if (running) {
                    chronometer.update()
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
