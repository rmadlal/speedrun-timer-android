package il.ronmad.speedruntimer

import android.widget.EditText
import io.realm.Realm
import java.util.*

fun EditText.isValidForGame(realm: Realm): Boolean {
    return when {
        this.text.isNullOrBlank() -> {
            this.error = "Title must not be empty"
            true
        }
        realm.gameExists(this.text.toString()) -> {
            this.error = "This game already exists"
            true
        }
        else -> false
    }
}

fun EditText.isValidForCategory(game: Game): Boolean {
    return when {
        this.text.isNullOrBlank() -> {
            this.error = "Category must not be empty"
            true
        }
        game.categoryExists(this.text.toString()) -> {
            this.error = "This category already exists"
            true
        }
        else -> false
    }
}

fun Long.getTimeUnits(): IntArray {
    val hours = this.toInt() / (1000 * 3600)
    var remaining = (this % (3600 * 1000)).toInt()
    val minutes = remaining / (60 * 1000)
    remaining %= (60 * 1000)
    val seconds = remaining / 1000
    val millis = remaining % 1000
    return intArrayOf(hours, minutes, seconds, millis)
}

fun Long.getFormattedTime(): String {
    val units = Math.abs(this).getTimeUnits()
    val hours = units[0]
    val minutes = units[1]
    val seconds = units[2]
    val millis = units[3] / 10
    var formattedTime = when {
        hours > 0 -> String.format(Locale.getDefault(), "%d:%02d:%02d.%02d", hours, minutes, seconds, millis)
        minutes > 0 -> String.format(Locale.getDefault(), "%d:%02d.%02d", minutes, seconds, millis)
        else -> String.format(Locale.getDefault(), "%d.%02d", seconds, millis)
    }
    if (this < 0) {
        formattedTime = "-" + formattedTime
    }
    return formattedTime
}

fun Long.setEditTextsFromTime(hoursInput: EditText,
                              minutesInput: EditText,
                              secondsInput: EditText,
                              millisInput: EditText) {
    val units = this.getTimeUnits()
    val hours = units[0]
    val minutes = units[1]
    val seconds = units[2]
    val millis = units[3]
    hoursInput.setText(if (hours > 0) "" + hours else "")
    minutesInput.setText(if (minutes > 0) "" + minutes else "")
    secondsInput.setText(if (seconds > 0) "" + seconds else "")
    millisInput.setText(if (millis > 0) "" + millis else "")
}

fun Int.toOrdinal(): String {
    return "$this${ when {
        this in 10..19 -> "th"
        this % 10 == 1 -> "st"
        this % 10 == 2 -> "nd"
        this % 10 == 3 -> "rd"
        else -> "th"
    }}"
}
