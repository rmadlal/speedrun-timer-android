package il.ronmad.speedruntimer

import android.annotation.TargetApi
import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import androidx.preference.PreferenceManager
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import il.ronmad.speedruntimer.activities.MainActivity
import il.ronmad.speedruntimer.realm.*
import io.realm.Realm
import io.realm.RealmChangeListener
import kotlinx.android.synthetic.main.timer_overlay.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.pow

class TimerService : Service() {

    private lateinit var realm: Realm
    private lateinit var realmChangeListener: RealmChangeListener<Realm>
    lateinit var prefs: SharedPreferences
    private lateinit var chronometer: Chronometer
    private lateinit var category: Category
    private var splitsIter: MutableListIterator<Split>? = null
    private val segmentTimes: MutableList<Long> = mutableListOf()
    private var currentSplitStartTime = 0L
    private var hasSplits = false
    private var comparison: Comparison = Comparison.PERSONAL_BEST

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var receiver: BroadcastReceiver

    lateinit var mView: View
    private lateinit var mWindowManager: WindowManager
    private lateinit var mWindowParams: WindowManager.LayoutParams
    private var moved = false

    private var startedProperly = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        realm = Realm.getDefaultInstance()
        realmChangeListener = RealmChangeListener { onDataChange() }
        realm.addChangeListener(realmChangeListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        IS_ACTIVE = true

        intent?.let {
            gameName = it.getStringExtra(getString(R.string.extra_game)).orEmpty()
            categoryName = it.getStringExtra(getString(R.string.extra_category)).orEmpty()
        }
        if (gameName.isEmpty() || categoryName.isEmpty()) {
            stopSelf()
            startedProperly = false
            return START_NOT_STICKY
        }

        category = realm.getCategoryByName(gameName, categoryName)!!
        hasSplits = category.splits.isNotEmpty()

        setupReceiver()
        val notification = setupNotification()
        startForeground(R.integer.notification_id, notification)

        setupChronometerPrefs()

        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupLayoutComponents()
        setupView()

        startedProperly = true
        IS_ACTIVE = true
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        realm.removeChangeListener(realmChangeListener)
        if (startedProperly) {
            mWindowManager.removeView(mView)
            unregisterReceiver(receiver)
        }
        realm.close()
        IS_ACTIVE = false
        super.onDestroy()
    }

    fun closeTimer(fromOnResume: Boolean) {
        if (Chronometer.started) {
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            Dialogs.showTimerActiveDialog(this, fromOnResume) { stopSelf() }
        } else {
            stopSelf()
        }
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
        Chronometer.apply {
            colorNeutral = prefs.getInt(getString(R.string.key_pref_color_neutral),
                    getColorCpt(R.color.colorTimerNeutralDefault))
            colorAhead = prefs.getInt(getString(R.string.key_pref_color_ahead),
                    getColorCpt(R.color.colorTimerAheadDefault))
            colorBehind = prefs.getInt(getString(R.string.key_pref_color_behind),
                    getColorCpt(R.color.colorTimerBehindDefault))
            colorPB = prefs.getInt(getString(R.string.key_pref_color_pb),
                    getColorCpt(R.color.colorTimerPBDefault))
            colorBestSegment = prefs.getInt(getString(R.string.key_pref_color_best_segment),
                    getColorCpt(R.color.colorTimerBestSegmentDefault))
            countdown = prefs.getLong(getString(R.string.key_pref_timer_countdown), 0L)
            showMillis = prefs.getBoolean(getString(R.string.key_pref_timer_show_millis), true)
            alwaysMinutes = prefs.getBoolean(getString(R.string.key_pref_timer_always_minutes), true)
        }
        comparison = getComparison()
    }

    private fun setupReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    getString(R.string.action_close_timer) ->
                        closeTimer(intent.getBooleanExtra(getString(R.string.extra_close_timer_from_onresume), true))
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(getString(R.string.action_close_timer))
        registerReceiver(receiver, intentFilter)
    }

    private fun setupNotification(): Notification {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
                .setSmallIcon(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    R.drawable.ic_timer_black_48dp
                else
                    R.drawable.ic_stat_timer)
                .setContentTitle("${category.gameName} ${category.name}")
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    R.drawable.ic_close_black_24dp
                else
                    R.drawable.ic_stat_close,
                        getString(R.string.close_timer),
                        PendingIntent.getBroadcast(this, 0,
                                Intent(getString(R.string.action_close_timer)).also {
                                    it.putExtra(getString(R.string.extra_close_timer_from_onresume), false)
                                },
                                PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
        if (category.bestTime > 0) {
            notificationBuilder.setContentText("PB: ${category.bestTime.getFormattedTime()}")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotificationChannel()
        }
        return notificationBuilder.build().also { notification ->
            notificationManager.notify(R.integer.notification_id, notification)
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun setupNotificationChannel() {
        val notificationChannel = NotificationChannel(
                getString(R.string.notification_channel_id),
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
        ).also {
            it.enableVibration(false)
            it.enableLights(false)
        }
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun setupLayoutComponents() {
        setTheme(R.style.AppTheme)
        mView = View.inflate(this, R.layout.timer_overlay, null)

        setupDisplayPrefs()

        chronometer = Chronometer(mView)

        mView.isLongClickable = true
        mView.setOnTouchListener(object : View.OnTouchListener {
            private val metrics = DisplayMetrics()
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0.toFloat()
            private var initialTouchY: Float = 0.toFloat()

            var touchTime: Long = 0
            var startTime = System.currentTimeMillis()
            var prevTapTime: Long = 0

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
                        // Prevent accidental double-clicking
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - prevTapTime < 400) return false
                        prevTapTime = currentTime

                        val splitTime = chronometer.timeElapsed
                        if (Chronometer.running) {
                            if (splitTime < 0) return false
                            if (hasSplits) {
                                // Split, or stop if on final split.
                                val segmentTime = splitTime - currentSplitStartTime
                                segmentTimes += segmentTime
                                if (prefs.getBoolean(getString(R.string.key_pref_timer_show_delta), true))
                                    getCurrentSplit()?.let { updateDelta(it, splitTime) }

                                if (splitsIter?.hasNext() == true) {
                                    timerSplit()
                                } else {
                                    timerStop()
                                }
                            } else {
                                // No splits, so just stop.
                                timerStop()
                            }
                        } else {
                            if (splitsIter == null)
                                timerStart()
                        }
                        currentSplitStartTime = splitTime.coerceAtLeast(0L)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val targetX = (initialX - (event.rawX - initialTouchX).toInt())
                                .coerceIn(0, metrics.widthPixels - v.width)
                        val targetY = (initialY - (event.rawY - initialTouchY).toInt())
                                .coerceIn(0, metrics.heightPixels - v.height)
                        if (moved || (targetX - initialX).toDouble().pow(2.0) + (targetY - initialY).toDouble().pow(2.0) >= 30 * 30) {
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
            val updateData = prefs.getBoolean(getString(R.string.key_pref_save_time_data), true)
            when {
                moved -> return@setOnLongClickListener false
                time <= 0 -> timerReset(updateData = false)
                Chronometer.running || (category.bestTime > 0 && category.bestTime in 0..time) ->
                    timerReset(updateData = updateData)
                category.bestTime == 0L -> timerReset(time, updateData = updateData)
                !updateData -> timerReset(updateData = false)
                else -> {
                    MaterialDialog(this).show {
                        title(text = "New personal best!")
                        val format = "%-16s%s"
                        message(text = format.format("Previous PB:", category.bestTime.getFormattedTime()) +
                                "\n" + format.format("Improvement:", (time - category.bestTime).getFormattedTime()))
                        checkBoxPrompt(text = if (hasSplits) "Save splits" else "Save", isCheckedDefault = true) {}
                        positiveButton(R.string.reset) {
                            timerReset(time, updateData = isCheckPromptChecked())
                        }
                        negativeButton(android.R.string.cancel)
                        window?.setType(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        else
                            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
                    }
                }
            }
            true
        }
    }

    private fun setupDisplayPrefs() {
        mView.setBackgroundColor(prefs.getInt(getString(R.string.key_pref_color_background),
                getColorCpt(R.color.colorTimerBackgroundDefault)))

        setupSize()
        setupFont()

        mView.currentSplit.setTextColor(Chronometer.colorNeutral)
    }

    private fun setupSize() {
        val timerSizesVals = resources.getStringArray(R.array.timer_sizes_values)
        val size = prefs.getString(getString(R.string.key_pref_timer_size), timerSizesVals[1])!!.toFloat()
        val millisSize = size * 0.75f
        val deltaSize = size * 0.375f
        val splitSize = size * 0.5f
        mView.apply {
            chronoViewSet.forEach {
                it.textSize = size
            }
            chronoDot.textSize = millisSize
            chronoMilli2.textSize = millisSize
            chronoMilli1.textSize = millisSize
            delta.textSize = deltaSize
            currentSplit.textSize = splitSize
        }
    }

    private fun setupFont() {
        val digital7Font = Typeface.createFromAsset(assets, "fonts/digital-7.ttf")
        mView.chronoViewSet.forEach { it.typeface = digital7Font }
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
        mWindowParams.x = x.coerceIn(0, metrics.widthPixels - mWindowParams.width)
        mWindowParams.y = y.coerceIn(0, metrics.heightPixels - mWindowParams.height)
        mWindowManager.updateViewLayout(mView, mWindowParams)
    }

    private fun timerReset(newPB: Long = 0L, updateData: Boolean = true) {
        chronometer.reset()
        if (updateData) {
            if (newPB == 0L) {
                category.incrementRunCount()
                category.updateSplits(segmentTimes, false)
            } else {
                category.updateData(bestTime = newPB, runCount = category.runCount + 1)
                category.updateSplits(segmentTimes, true)
                FSTWidget.forceUpdateWidgets(this)
            }
        }
        resetSplits()
        resetTimerPosition()
    }

    private fun resetSplits() {
        segmentTimes.clear()
        splitsIter = null
        mView.delta.visibility = View.GONE
        mView.currentSplit.visibility = View.GONE
    }

    private fun timerSplit() {
        splitsIter?.next()?.let {
            chronometer.compareAgainst = if (it.hasTime(comparison)) it.calculateSplitTime(comparison)
            else 0L
            mView.currentSplit.text = it.name
        }
    }

    private fun timerStart() {
        if (hasSplits) {
            splitsIter = category.splits.listIterator()
            timerSplit()
            mView.currentSplit.visibility =
                    if (prefs.getBoolean(getString(R.string.key_pref_timer_show_current_split), true))
                        View.VISIBLE
                    else View.GONE
        } else {
            chronometer.compareAgainst = category.bestTime
        }
        chronometer.start()
    }

    private fun timerStop() {
        chronometer.stop()
        mView.currentSplit.visibility = View.GONE
    }

    private fun getCurrentSplit(): Split? {
        splitsIter?.previous()
        return splitsIter?.next()
    }

    private fun updateDelta(currentSplit: Split, splitTime: Long) {
        val segmentTime = splitTime - currentSplitStartTime
        val delta = splitTime - currentSplit.calculateSplitTime(comparison)
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

        fun launchTimer(
                context: Context?,
                gameName: String,
                categoryName: String,
                minimizeIfNoGameLaunch: Boolean = true
        ) = GlobalScope.launch(Dispatchers.Main) {
            context ?: return@launch
            if (IS_ACTIVE) {
                context.showToast(context.getString(R.string.toast_close_active_timer))
                return@launch
            }
            if (!context.tryLaunchGame(gameName)) {
                if (minimizeIfNoGameLaunch)
                    context.minimizeApp()
            }
            context.startTimerService(gameName, categoryName)
        }
    }
}
