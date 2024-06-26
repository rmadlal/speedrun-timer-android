package il.ronmad.speedruntimer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ExpandableListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.gson.stream.JsonReader
import il.ronmad.speedruntimer.activities.MainActivity
import il.ronmad.speedruntimer.databinding.EditTimeLayoutBinding
import il.ronmad.speedruntimer.databinding.TimerOverlayBinding
import il.ronmad.speedruntimer.realm.*
import il.ronmad.speedruntimer.web.Failure
import il.ronmad.speedruntimer.web.Result
import il.ronmad.speedruntimer.web.SplitsIO
import il.ronmad.speedruntimer.web.Success
import io.realm.Realm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.abs

fun EditText.isValidForGame(realm: Realm): Boolean {
    return when {
        text.isNullOrBlank() -> {
            error = "Title must not be empty"
            requestFocus()
            false
        }
        realm.gameExists(text.toString()) -> {
            error = "This game already exists"
            requestFocus()
            false
        }
        else -> true
    }
}

fun EditText.isValidForCategory(game: Game): Boolean {
    return when {
        text.isNullOrBlank() -> {
            error = "Category must not be empty"
            requestFocus()
            false
        }
        game.categoryExists(text.toString()) -> {
            error = "This category already exists"
            requestFocus()
            false
        }
        else -> true
    }
}

fun EditText.isValidForSplit(category: Category): Boolean {
    return when {
        text.isNullOrBlank() -> {
            error = "Split name must not be empty"
            requestFocus()
            false
        }
        category.splitExists(text.toString()) -> {
            error = "This split already exists"
            requestFocus()
            false
        }
        else -> true
    }
}

inline fun EditText.onTextChanged(crossinline listener: (CharSequence?) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            listener(s)
        }
    })
}

fun Long.getTimeUnits(twoDecimalPlaces: Boolean = false): IntArray {
    val time = abs(this)
    val hours = time.toInt() / (1000 * 3600)
    var remaining = (time % (3600 * 1000)).toInt()
    val minutes = remaining / (60 * 1000)
    remaining %= (60 * 1000)
    val seconds = remaining / 1000
    val millis = if (twoDecimalPlaces) ((remaining % 1000) / 10) else (remaining % 1000)
    return intArrayOf(hours, minutes, seconds, millis)
}

fun Long.getFormattedTime(
    withMillis: Boolean = true,
    forceMinutes: Boolean = false,
    plusSign: Boolean = false,
    dashIfZero: Boolean = false
): String {
    if (dashIfZero && this == 0L) return "-"
    val (hours, minutes, seconds, millis) = getTimeUnits(true)
    val formattedTime = when {
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes, seconds)
        minutes > 0 || forceMinutes -> "%d:%02d".format(minutes, seconds)
        else -> "%d".format(seconds)
    } + if (withMillis) ".%02d".format(millis) else ""
    return when {
        this < 0 -> "-$formattedTime"
        plusSign -> "+$formattedTime"
        else -> formattedTime
    }
}

fun EditTimeLayoutBinding.setEditTextsFromTime(time: Long) {
    val (hours, minutes, seconds, millis) = time.getTimeUnits()
    this.hours.setText(if (hours > 0) hours.toString() else "")
    this.minutes.setText(if (minutes > 0) minutes.toString() else "")
    this.seconds.setText(if (seconds > 0) seconds.toString() else "")
    this.milliseconds.setText(if (millis > 0) millis.toString() else "")
}

fun EditTimeLayoutBinding.getTimeFromEditTexts(): Long {
    val hoursStr = this.hours.text.toString()
    val minutesStr = this.minutes.text.toString()
    val secondsStr = this.seconds.text.toString()
    val millisStr = this.milliseconds.text.toString()
    val hours = if (hoursStr.isNotEmpty()) hoursStr.toInt() else 0
    val minutes = if (minutesStr.isNotEmpty()) minutesStr.toInt() else 0
    val seconds = if (secondsStr.isNotEmpty()) secondsStr.toInt() else 0
    val millis = if (millisStr.isNotEmpty()) millisStr.toInt() else 0
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

val Context?.app
    get() = this?.applicationContext as? MyApplication

fun Context.getColorCpt(color: Int) = ContextCompat.getColor(this, color)

fun Context.pixelToDp(pixels: Float): Int {
    val scale = resources.displayMetrics.density
    return (pixels * scale + 0.5f).toInt()
}

fun Context.startTimerService(gameName: String, categoryName: String) {
    val serviceIntent = Intent(this, TimerService::class.java).also {
        it.putExtra(getString(R.string.extra_game), gameName)
        it.putExtra(getString(R.string.extra_category), categoryName)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(serviceIntent)
    } else {
        startService(serviceIntent)
    }
}

fun Context.minimizeApp() {
    val homeIntent = Intent(Intent.ACTION_MAIN).also {
        it.addCategory(Intent.CATEGORY_HOME)
        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(homeIntent)
}

suspend fun Context.tryLaunchGame(gameName: String): Boolean {
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
    if (!sharedPrefs.getBoolean(getString(R.string.key_pref_launch_games), true)) {
        return false
    }
    val app = this.app ?: return false
    withContext(Dispatchers.Default) {
        app.setupInstalledAppsMap()
    }
    app.installedAppsMap[gameName.lowercase(Locale.US)]?.let {
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

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long) =
    fold(0L) { acc, curr -> acc + selector(curr) }

/**
 * Converts Run to Category, adding it to Realm.
 * This overwrites the category's splits if they exist.
 */
fun SplitsIO.Run.toRealmCategory(
    gameName: String = this.gameName,
    categoryName: String = this.categoryName
): Category {
    return withRealm {
        val game = getGameByName(gameName) ?: addGame(gameName)
        val category = game.getCategoryByName(categoryName) ?: game.addCategory(categoryName)
        category.apply {
            executeTransaction { splits.deleteAllFromRealm() }
            segments.forEach {
                addSplit(it.segmentName)
                    .updateData(pbTime = it.pbDuration, bestTime = it.bestDuration)
            }
            setPBFromSplits()
            updateData(runCount = attemptsTotal)
        }
    }
}

/**
 * Convenience method for reading a Json Object from which only one field is needed.
 * @throws IllegalArgumentException if the object does not contain property [name]
 */
inline fun <reified T> JsonReader.readSingleObjectValue(name: String): T {
    var value: T? = null
    beginObject()
    while (hasNext()) {
        when (nextName()) {
            name -> value = nextValue()
            else -> skipValue()
        }
    }
    endObject()
    return value ?: throw IllegalArgumentException("Property $name not found in object")
}

/**
 * @throws IllegalArgumentException if the passed type is not supported by a next*() method
 */
inline fun <reified T> JsonReader.nextValue(): T {
    return when (T::class) {
        String::class -> nextString() as T
        Boolean::class -> nextBoolean() as T
        Unit::class -> nextNull() as T
        Double::class -> nextDouble() as T
        Long::class -> nextLong() as T
        Int::class -> nextInt() as T
        else -> throw IllegalArgumentException("Invalid type")
    }
}

fun ExpandableListView.getExpandedGroupPositions(): List<Int> =
    (0 until count).filter { isGroupExpanded(it) }

/**
 * Wraps the receiver in a Success if not null, or Failure otherwise
 */
fun <T> T?.toResult(): Result<T> = this?.let { Success(it) } ?: Failure()

val TimerOverlayBinding.chronoViewSet: Set<TextView>
    get() {
        return setOf(
            chronoMinus,
            chronoHr2,
            chronoHr1,
            chronoHrMinColon,
            chronoMin2,
            chronoMin1,
            chronoMinSecColon,
            chronoSec2,
            chronoSec1,
            chronoDot,
            chronoMilli2,
            chronoMilli1
        )
    }
