package il.ronmad.speedruntimer

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AlertDialog
import android.util.DisplayMetrics
import android.view.*

import io.realm.Realm
import io.realm.RealmChangeListener
import kotlinx.android.synthetic.main.timer_overlay.view.*

class TimerService : Service(), TimeExtensions {

    private lateinit var realm: Realm
    private lateinit var realmChangeListener: RealmChangeListener<Realm>
    private lateinit var prefs: SharedPreferences
    private lateinit var chronometer: Chronometer
    private lateinit var category: Category
    private lateinit var splitsIter: MutableListIterator<Split>
    private var segmentTimes = mutableListOf<Long>()
    private lateinit var currentSplit: Split
    private var currentSplitStartTime = 0L
    private var currentSegmentSplitTime = 0L
    private var hasSplits = false
    private var comparison: Comparison = Comparison.PERSONAL_BEST

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    private lateinit var mView: View
    private lateinit var mWindowManager: WindowManager
    private lateinit var mWindowParams: WindowManager.LayoutParams
    private var moved = false

    private var startedProperly = false

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        realm = Realm.getDefaultInstance()
        realmChangeListener = RealmChangeListener { onDataChange() }
        realm.addChangeListener(realmChangeListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        IS_ACTIVE = true

        intent?.let {
            gameName = it.getStringExtra(getString(R.string.extra_game))
            categoryName = it.getStringExtra(getString(R.string.extra_category))
        }
        if (gameName.isEmpty() || categoryName.isEmpty()) {
            stopSelf()
            startedProperly = false
            return START_NOT_STICKY
        }

        category = realm.getCategoryByName(gameName, categoryName)!!
        hasSplits = category.splits.isNotEmpty()

        val notification = setupNotification()
        startForeground(R.integer.notification_id, notification)

        setupChronometerPrefs()

        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupLayoutComponents()
        setupView()

        startedProperly = true
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        realm.removeChangeListener(realmChangeListener)
        if (startedProperly) {
            mWindowManager.removeView(mView)
        }
        realm.close()
        IS_ACTIVE = false
        super.onDestroy()
    }

    private fun onDataChange() {
        notificationBuilder.setContentText(if (category.bestTime > 0)
            "PB: ${category.bestTime.getFormattedTime()}"
        else
            null)
        notificationManager.notify(R.integer.notification_id, notificationBuilder.build())
    }

    private fun setupChronometerPrefs() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        Chronometer.colorNeutral = prefs.getInt(getString(R.string.key_pref_color_neutral),
                getColorCpt(R.color.colorTimerNeutralDefault))
        Chronometer.colorAhead = prefs.getInt(getString(R.string.key_pref_color_ahead),
                getColorCpt(R.color.colorTimerAheadDefault))
        Chronometer.colorBehind = prefs.getInt(getString(R.string.key_pref_color_behind),
                getColorCpt(R.color.colorTimerBehindDefault))
        Chronometer.colorPB = prefs.getInt(getString(R.string.key_pref_color_pb),
                getColorCpt(R.color.colorTimerPBDefault))
        Chronometer.colorBestSegment = prefs.getInt(getString(R.string.key_pref_color_best_segment),
                getColorCpt(R.color.colorTimerBestSegmentDefault))
        Chronometer.countdown = prefs.getLong(getString(R.string.key_pref_timer_countdown), 0L)
        Chronometer.showMillis = prefs.getBoolean(getString(R.string.key_pref_timer_show_millis), true)
        comparison = getComparison()
    }

    private fun setupNotification(): Notification {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
                .setSmallIcon(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    R.drawable.ic_timer_black_48dp
                else
                    R.drawable.ic_stat_timer)
                .setContentTitle("${category.getGame().name} ${category.name}")
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    R.drawable.ic_close_black_24dp
                else
                    R.drawable.ic_stat_close,
                        getString(R.string.close_timer),
                        PendingIntent.getBroadcast(this, 0,
                                Intent(getString(R.string.action_close_timer)),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
        if (category.bestTime > 0) {
            notificationBuilder.setContentText("PB: ${category.bestTime.getFormattedTime()}")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotificationChannel()
        }
        val notification = notificationBuilder.build()
        notificationManager.notify(R.integer.notification_id, notification)
        return notification
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun setupNotificationChannel() {
        val notificationChannel = NotificationChannel(
                getString(R.string.notification_channel_id),
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW)
        notificationChannel.enableVibration(false)
        notificationChannel.enableLights(false)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun setupLayoutComponents() {
        setTheme(R.style.AppTheme)
        mView = View.inflate(this, R.layout.timer_overlay, null)

        setupDisplayPrefs()

        chronometer = Chronometer(this, mView)

        mView.isLongClickable = true
        mView.setOnTouchListener(object : View.OnTouchListener {
            private val metrics = DisplayMetrics()
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0.toFloat()
            private var initialTouchY: Float = 0.toFloat()

            internal var touchTime: Long = 0
            internal var startTime = System.currentTimeMillis()

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (System.currentTimeMillis() - startTime <= 300) {
                    return false
                }

                mWindowManager.defaultDisplay.getMetrics(metrics)

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = mWindowParams.x
                        initialY = mWindowParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        touchTime = System.currentTimeMillis()
                        moved = false
                    }
                    MotionEvent.ACTION_UP -> {
                        if (moved)
                            category.getGame().getPosition().set(mWindowParams.x, mWindowParams.y)
                        if (moved || System.currentTimeMillis() - touchTime >= 250) return false
                        if (Chronometer.running) {
                            if (chronometer.timeElapsed < 0) return false
                            if (hasSplits) {
                                // Split, or stop if on final split.
                                val splitTime = chronometer.timeElapsed
                                val segmentTime = splitTime - currentSplitStartTime
                                segmentTimes.add(segmentTime)
                                if (prefs.getBoolean(getString(R.string.key_pref_timer_show_delta), true))
                                    updateDelta(splitTime, segmentTime)

                                if (splitsIter.hasNext()) {
                                    timerSplit(splitTime)
                                } else {
                                    chronometer.stop()
                                    mView.currentSplit.visibility = View.GONE
                                }
                            } else {
                                // No splits, so just stop.
                                chronometer.stop()
                            }
                        } else {
                            timerStart()
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        var targetX = initialX - (event.rawX - initialTouchX).toInt()
                        var targetY = initialY - (event.rawY - initialTouchY).toInt()
                        targetX = Math.max(0, Math.min(targetX, metrics.widthPixels - v.width))
                        targetY = Math.max(0, Math.min(targetY, metrics.heightPixels - v.height))
                        if (moved || Math.pow((targetX - initialX).toDouble(), 2.0) + Math.pow((targetY - initialY).toDouble(), 2.0) >= 30 * 30) {
                            moved = true
                            mWindowParams.x = targetX
                            mWindowParams.y = targetY
                            mWindowManager.updateViewLayout(mView, mWindowParams)
                        }
                    }
                }
                v.performClick()
                return false
            }
        })

        mView.setOnLongClickListener {
            val time = chronometer.timeElapsed
            if (moved) {
                return@setOnLongClickListener false
            }
            if (time <= 0) {
                timerReset(updateData = false)
            } else if (Chronometer.running || (category.bestTime > 0 && category.bestTime in 0..time)) {
                timerReset()
            } else if (category.bestTime == 0L) {
                timerReset(time)
            } else {
                if (!prefs.getBoolean(getString(R.string.key_pref_save_time_data), true)) {
                    timerReset(updateData = false)
                    return@setOnLongClickListener true
                }
                val resetDialog = AlertDialog.Builder(this)
                        .setTitle(if (category.bestTime == 0L)
                            "New personal best!"
                        else
                            "New personal best! (${(time - category.bestTime).getFormattedTime()})")
                        .setMessage("Save it?")
                        .setPositiveButton(R.string.save_reset) { _, _ ->
                            timerReset(time)
                        }
                        .setNegativeButton(R.string.reset) { _, _ ->
                            timerReset()
                        }
                        .setNeutralButton(android.R.string.cancel, null)
                        .create()
                resetDialog.window?.setType(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
                resetDialog.show()
            }
            true
        }
    }

    private fun setupDisplayPrefs() {
        mView.setBackgroundColor(prefs.getInt(getString(R.string.key_pref_color_background),
                getColorCpt(R.color.colorTimerBackgroundDefault)))
        val timerSizesVals = resources.getStringArray(R.array.timer_sizes_values)
        val size = prefs.getString(getString(R.string.key_pref_timer_size), timerSizesVals[1]).toFloat()
        mView.chronoRest.textSize = size
        mView.chronoMillis.textSize = size * 0.75f
        mView.delta.textSize = size * 0.375f
        mView.currentSplit.textSize = size * 0.5f

        mView.currentSplit.setTextColor(Chronometer.colorNeutral)

        mView.chronoRest.typeface = Typeface.createFromAsset(
                assets, "fonts/digital-7.ttf")
        mView.chronoMillis.typeface = Typeface.createFromAsset(
                assets, "fonts/digital-7.ttf")
    }

    private fun setupView() {
        val metrics = DisplayMetrics()
        mWindowManager.defaultDisplay.getMetrics(metrics)
        mWindowParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)
        mWindowParams.gravity = Gravity.BOTTOM or Gravity.END

        mWindowManager.addView(mView, mWindowParams)
        resetTimerPosition()
    }

    private fun resetTimerPosition() {
        val (x, y) = category.getGame().getPosition()
        val metrics = DisplayMetrics()
        mWindowManager.defaultDisplay.getMetrics(metrics)
        mWindowParams.x = Math.max(0, Math.min(x, metrics.widthPixels - mWindowParams.width))
        mWindowParams.y = Math.max(0, Math.min(y, metrics.heightPixels - mWindowParams.height))
        mWindowManager.updateViewLayout(mView, mWindowParams)
    }

    private fun timerReset(newPB: Long = 0L, updateData: Boolean = true) {
        chronometer.reset()
        currentSegmentSplitTime = 0L
        if (updateData) {
            if (newPB == 0L) {
                category.incrementRunCount()
                category.updateSplits(segmentTimes, false)
            } else {
                category.updateData(bestTime = newPB, runCount = category.runCount + 1)
                category.updateSplits(segmentTimes, true)
            }
        }
        resetSplits()
        resetTimerPosition()
    }

    private fun resetSplits() {
        splitsIter = category.splits.listIterator()
        segmentTimes.clear()
        mView.delta.visibility = View.GONE
        mView.currentSplit.visibility = View.GONE
    }

    private fun timerSplit(splitTime: Long) {
        currentSplit = splitsIter.next()
        currentSegmentSplitTime = currentSplit.calculateSplitTime(comparison)
        currentSplitStartTime = splitTime
        chronometer.split(if (!currentSplit.hasTime(comparison)) 0L else currentSegmentSplitTime)
        mView.currentSplit.text = currentSplit.name
    }

    private fun timerStart() {
        if (hasSplits) {
            splitsIter = category.splits.listIterator()
            timerSplit(0L)
            mView.currentSplit.visibility =
                    if (prefs.getBoolean(getString(R.string.key_pref_timer_show_current_split), true))
                        View.VISIBLE
                    else View.GONE
        } else {
            currentSegmentSplitTime = category.bestTime
        }
        chronometer.start(currentSegmentSplitTime)
    }

    private fun updateDelta(splitTime: Long, segmentTime: Long) {
        val delta = splitTime - currentSegmentSplitTime
        mView.delta.text = delta.getFormattedTime(plusSign = true)
        mView.delta.setTextColor(when {
            segmentTime < currentSplit.bestTime -> Chronometer.colorBestSegment
            delta < 0 -> Chronometer.colorAhead
            else -> Chronometer.colorBehind
        })
        mView.delta.visibility = if (!currentSplit.hasTime(comparison)) View.GONE else View.VISIBLE
    }

    companion object {

        var IS_ACTIVE = false
        var gameName = ""
        var categoryName = ""
    }
}
