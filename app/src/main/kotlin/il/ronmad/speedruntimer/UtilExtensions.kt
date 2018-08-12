package il.ronmad.speedruntimer

import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.EditText
import io.realm.Realm
import kotlinx.android.synthetic.main.edit_time_layout.view.*

fun EditText.isValidForGame(realm: Realm): Boolean {
    return when {
        this.text.isNullOrBlank() -> {
            this.error = "Title must not be empty"
            false
        }
        realm.gameExists(this.text.toString()) -> {
            this.error = "This game already exists"
            false
        }
        else -> true
    }
}

fun EditText.isValidForCategory(game: Game): Boolean {
    return when {
        this.text.isNullOrBlank() -> {
            this.error = "Category must not be empty"
            false
        }
        game.categoryExists(this.text.toString()) -> {
            this.error = "This category already exists"
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
    var formattedTime = when {
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
    if (this < 0) {
        formattedTime = "-$formattedTime"
    } else if (plusSign) {
        formattedTime = "+$formattedTime"
    }
    return formattedTime
}

fun Long.setEditTextsFromTime(containingView: View) {
    val (hours, minutes, seconds, millis) = getTimeUnits()
    containingView.hours.setText(if (hours > 0) "$hours" else "")
    containingView.minutes.setText(if (minutes > 0) "$minutes" else "")
    containingView.seconds.setText(if (seconds > 0) "$seconds" else "")
    containingView.milliseconds.setText(if (millis > 0) "$millis" else "")
}

fun View.getTimeFromEditTexts(): Long {
    val hoursStr = this.hours.text.toString()
    val minutesStr = this.minutes.text.toString()
    val secondsStr = this.seconds.text.toString()
    val millisStr = this.milliseconds.text.toString()
    val hours = if (hoursStr.isEmpty()) 0 else Integer.parseInt(hoursStr)
    val minutes = if (minutesStr.isEmpty()) 0 else Integer.parseInt(minutesStr)
    val seconds = if (secondsStr.isEmpty()) 0 else Integer.parseInt(secondsStr)
    val millis = if (millisStr.isEmpty()) 0 else Integer.parseInt(millisStr)
    return (1000 * 60 * 60 * hours + 1000 * 60 * minutes + 1000 * seconds + millis).toLong()
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

fun Context.getColorCpt(color: Int) = ContextCompat.getColor(this, color)

fun Context.pixelToDp(pixels: Float): Int {
    val scale = resources.displayMetrics.density
    return (pixels * scale + 0.5f).toInt()
}

fun Context.minimizeApp() {
    val homeIntent = Intent(Intent.ACTION_MAIN)
    homeIntent.addCategory(Intent.CATEGORY_HOME)
    startActivity(homeIntent)
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
