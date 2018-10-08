package il.ronmad.speedruntimer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.EditText
import android.widget.ExpandableListView
import android.widget.Toast
import com.google.gson.stream.JsonReader
import il.ronmad.speedruntimer.activities.MainActivity
import il.ronmad.speedruntimer.realm.*
import il.ronmad.speedruntimer.web.SplitsIO
import io.realm.Realm
import kotlinx.android.synthetic.main.edit_time_layout.view.*
import kotlinx.coroutines.Job

fun EditText.isValidForGame(realm: Realm): Boolean {
    return when {
        this.text.isNullOrBlank() -> {
            this.error = "Title must not be empty"
            requestFocus()
            false
        }
        realm.gameExists(this.text.toString()) -> {
            this.error = "This game already exists"
            requestFocus()
            false
        }
        else -> true
    }
}

fun EditText.isValidForCategory(game: Game): Boolean {
    return when {
        this.text.isNullOrBlank() -> {
            this.error = "Category must not be empty"
            requestFocus()
            false
        }
        game.categoryExists(this.text.toString()) -> {
            this.error = "This category already exists"
            requestFocus()
            false
        }
        else -> true
    }
}

fun EditText.isValidForSplit(category: Category): Boolean {
    return when {
        this.text.isNullOrBlank() -> {
            this.error = "Split name must not be empty"
            false
        }
        category.splitExists(this.text.toString()) -> {
            this.error = "This split already exists"
            false
        }
        else -> true
    }
}

fun Long.getTimeUnits(twoDecimalPlaces: Boolean = false): IntArray {
    val time = Math.abs(this)
    val hours = time.toInt() / (1000 * 3600)
    var remaining = (time % (3600 * 1000)).toInt()
    val minutes = remaining / (60 * 1000)
    remaining %= (60 * 1000)
    val seconds = remaining / 1000
    val millis = if (twoDecimalPlaces) ((remaining % 1000) / 10) else (remaining % 1000)
    return intArrayOf(hours, minutes, seconds, millis)
}

fun Long.getFormattedTime(withMillis: Boolean = true,
                          forceMinutes: Boolean = false,
                          plusSign: Boolean = false,
                          dashIfZero: Boolean = false): String {
    if (dashIfZero && this == 0L) return "-"
    val (hours, minutes, seconds, millis) = getTimeUnits(true)
    val formattedTime = when {
        hours > 0 ->
            if (withMillis) "%d:%02d:%02d.%02d".format(hours, minutes, seconds, millis)
            else "%d:%02d:%02d".format(hours, minutes, seconds)
        minutes > 0 || forceMinutes ->
            if (withMillis) "%d:%02d.%02d".format(minutes, seconds, millis)
            else "%d:%02d".format(minutes, seconds)
        else ->
            if (withMillis) "%d.%02d".format(seconds, millis)
            else "%d".format(seconds)
    }
    return when {
        this < 0 -> "-$formattedTime"
        plusSign -> "+$formattedTime"
        else -> formattedTime
    }
}

fun View.setEditTextsFromTime(time: Long) {
    requireTimeViewsNotNull(this)
    val (hours, minutes, seconds, millis) = time.getTimeUnits()
    this.hours.setText(if (hours > 0) hours.toString() else "")
    this.minutes.setText(if (minutes > 0) minutes.toString() else "")
    this.seconds.setText(if (seconds > 0) seconds.toString() else "")
    this.milliseconds.setText(if (millis > 0) millis.toString() else "")
}

fun View.getTimeFromEditTexts(): Long {
    requireTimeViewsNotNull(this)
    val hoursStr = this.hours.text.toString()
    val minutesStr = this.minutes.text.toString()
    val secondsStr = this.seconds.text.toString()
    val millisStr = this.milliseconds.text.toString()
    val hours = if (hoursStr.isNotEmpty()) Integer.parseInt(hoursStr) else 0
    val minutes = if (minutesStr.isNotEmpty()) Integer.parseInt(minutesStr) else 0
    val seconds = if (secondsStr.isNotEmpty()) Integer.parseInt(secondsStr) else 0
    val millis = if (millisStr.isNotEmpty()) Integer.parseInt(millisStr) else 0
    return (1000 * 60 * 60 * hours + 1000 * 60 * minutes + 1000 * seconds + millis).toLong()
}

private fun requireTimeViewsNotNull(view: View) {
    requireNotNull(view.hours)
    requireNotNull(view.minutes)
    requireNotNull(view.seconds)
    requireNotNull(view.milliseconds)
}

fun Int.toOrdinal(): String {
    val suffix = when {
        this in 10..19 -> "th"
        this % 10 == 1 -> "st"
        this % 10 == 2 -> "nd"
        this % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$this$suffix"
}

val Context?.app
    get() = this?.applicationContext as? MyApplication

fun Context.getColorCpt(color: Int) = ContextCompat.getColor(this, color)

fun Context.pixelToDp(pixels: Float): Int {
    val scale = resources.displayMetrics.density
    return (pixels * scale + 0.5f).toInt()
}

fun Context.startTimerService(gameName: String, categoryName: String) {
    val serviceIntent = Intent(this, TimerService::class.java)
    serviceIntent.putExtra(this.getString(R.string.extra_game), gameName)
    serviceIntent.putExtra(this.getString(R.string.extra_category), categoryName)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.startForegroundService(serviceIntent)
    } else {
        this.startService(serviceIntent)
    }
}

fun Context.minimizeApp() {
    val homeIntent = Intent(Intent.ACTION_MAIN)
    homeIntent.addCategory(Intent.CATEGORY_HOME)
    homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(homeIntent)
}

fun Context.tryLaunchGame(gameName: String): Boolean {
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
    if (!sharedPrefs.getBoolean(getString(R.string.key_pref_launch_games), true)) {
        return false
    }
    app?.installedApps?.get(gameName.toLowerCase())?.let {
        showToast("Launching ${packageManager.getApplicationLabel(it)}...")
        startActivity(packageManager.getLaunchIntentForPackage(it.packageName))
        return true
    }
    return false
}

fun Context.showToast(text: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, length).show()
}

fun MainActivity.getComparison() = getComparison(this)

fun TimerService.getComparison() = getComparison(this)

private fun getComparison(context: Context): Comparison {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    return when (prefs.getString(context.getString(R.string.key_pref_compare_against), "0")) {
        // Personal Best
        "0" -> Comparison.PERSONAL_BEST
        // Best Segments
        "1" -> Comparison.BEST_SEGMENTS
        else -> Comparison.PERSONAL_BEST
    }
}

inline fun <T> Iterable<T>.sumBy(selector: (T) -> Long) =
        fold(0L) { acc, curr -> acc + selector(curr) }

suspend inline fun Job.then(block: () -> Unit) {
    join()
    block()
}

fun SplitsIO.Run.toRealmCategory(gameName: String = this.gameName,
                                 categoryName: String = this.categoryName): Category {
    return withRealm {
        val game = getGameByName(gameName) ?: addGame(gameName)
        val category = game.getCategoryByName(categoryName) ?: game.addCategory(categoryName)
        category.apply {
            this@withRealm.executeTransaction { splits.deleteAllFromRealm() }
            segments.forEach {
                addSplit(it.segmentName)
                        .updateData(pbTime = it.pbDuration, bestTime = it.bestDuration)
            }
            setPBFromSplits()
            updateData(runCount = attemptsTotal)
        }
    }
}

fun JsonReader.readSingleObjectValue(name: String): String {
    var value = ""
    beginObject()
    while (hasNext()) {
        when (nextName()) {
            name -> value = nextString()
            else -> skipValue()
        }
    }
    endObject()
    return value
}

fun ExpandableListView.getExpandedGroupPositions(): List<Int> =
        (0 until count).filter { isGroupExpanded(it) }