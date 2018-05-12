package il.ronmad.speedruntimer

import android.content.Context
import android.support.v4.content.ContextCompat
import android.widget.EditText
import io.realm.Realm

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
                          plusSign: Boolean = false): String {
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

fun Long.setEditTextsFromTime(hoursInput: EditText,
                              minutesInput: EditText,
                              secondsInput: EditText,
                              millisInput: EditText) {
    val (hours, minutes, seconds, millis) = getTimeUnits()
    hoursInput.setText(if (hours > 0) "$hours" else "")
    minutesInput.setText(if (minutes > 0) "$minutes" else "")
    secondsInput.setText(if (seconds > 0) "$seconds" else "")
    millisInput.setText(if (millis > 0) "$millis" else "")
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

inline operator fun <reified T> MyBaseListFragmentAdapter<T>.get(position: Int) =
        this.getItem(position)

fun Context.getColorCpt(color: Int) = ContextCompat.getColor(this, color)
