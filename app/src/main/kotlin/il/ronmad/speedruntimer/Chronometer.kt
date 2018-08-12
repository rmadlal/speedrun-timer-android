package il.ronmad.speedruntimer

import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.timer_overlay.view.*

import java.lang.ref.WeakReference

class Chronometer(private val timerService: TimerService,
                  private val chronoView: View,
                  private val chronoViewSet: Set<TextView>) {

    private var base = 0L

    internal var timeElapsed: Long = 0
        private set

    private var timerColor = colorNeutral
        set(value) {
            if (value == field) return
            chronoViewSet.forEach { it.setTextColor(value) }
            field = value
        }

    internal var compareAgainst = 0L
    private val chronoHandler: Handler

    init {
        if (!showMillis) {
            chronoView.chronoMilli1.visibility = View.GONE
            chronoView.chronoMilli2.visibility = View.GONE
            chronoView.dot.visibility = View.GONE
        }
        chronoViewSet.forEach { it.setTextColor(colorNeutral) }
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
        chronoView.chronoHr2.text = (hours / 10).toString()
        chronoView.chronoHr1.text = (hours % 10).toString()
        chronoView.chronoMin2.text = (minutes / 10).toString()
        chronoView.chronoMin1.text = (minutes % 10).toString()
        chronoView.chronoSec2.text = (seconds / 10).toString()
        chronoView.chronoSec1.text = (seconds % 10).toString()
        chronoView.chronoMilli2.text = (millis / 10).toString()
        chronoView.chronoMilli1.text = (millis % 10).toString()
    }

    private fun updateVisibility() {
        val (hours, minutes, seconds, _) = timeElapsed.getTimeUnits(true)
        chronoView.chronoMinus.visibility = if (timeElapsed < 0) View.VISIBLE else View.GONE
        when {
            hours > 0 -> {
                chronoView.chronoHr2.visibility = if (hours / 10 > 0) View.VISIBLE else View.GONE
                chronoView.chronoHr1.visibility = View.VISIBLE
                chronoView.hrMinColon.visibility = View.VISIBLE
                chronoView.chronoMin2.visibility = View.VISIBLE
                chronoView.chronoMin1.visibility = View.VISIBLE
                chronoView.minSecColon.visibility = View.VISIBLE
                chronoView.chronoSec2.visibility = View.VISIBLE
            }
            minutes > 0 -> {
                chronoView.chronoHr2.visibility = View.GONE
                chronoView.chronoHr1.visibility = View.GONE
                chronoView.hrMinColon.visibility = View.GONE
                chronoView.chronoMin2.visibility = if (minutes / 10 > 0) View.VISIBLE else View.GONE
                chronoView.chronoMin1.visibility = View.VISIBLE
                chronoView.minSecColon.visibility = View.VISIBLE
                chronoView.chronoSec2.visibility = View.VISIBLE
            }
            else -> {
                chronoView.chronoHr2.visibility = View.GONE
                chronoView.chronoHr1.visibility = View.GONE
                chronoView.hrMinColon.visibility = View.GONE
                chronoView.chronoMin2.visibility = View.GONE
                chronoView.chronoMin1.visibility = if (alwaysMinutes) View.VISIBLE else View.GONE
                chronoView.minSecColon.visibility = if (alwaysMinutes) View.VISIBLE else View.GONE
                chronoView.chronoSec2.visibility = if (alwaysMinutes || seconds / 10 > 0) View.VISIBLE else View.GONE
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

    private class ChronoHandler internal constructor(instance: Chronometer) : Handler() {

        private val instance: WeakReference<Chronometer> = WeakReference(instance)

        override fun handleMessage(m: Message) {
            val chronometer = instance.get()
            chronometer?.let {
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
