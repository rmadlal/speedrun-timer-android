package il.ronmad.speedruntimer

import android.widget.EditText

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object Util {

    internal fun migrateJson(json: String): String {
        val gamesArray = JsonParser().parse(json).asJsonArray
        for (gameElement in gamesArray) {
            val gameObject = gameElement.asJsonObject
            val categoriesObject = gameObject.get("categories").asJsonObject
            val categoriesArray = JsonArray()
            for ((key, value) in categoriesObject.entrySet()) {
                val categoryObject = JsonObject()
                categoryObject.addProperty("bestTime", value.asLong)
                categoryObject.addProperty("name", key)
                categoriesArray.add(categoryObject)
            }
            gameObject.add("categories", categoriesArray)
        }
        return gamesArray.toString()
    }

    internal fun getTimeFromEditTexts(hoursInput: EditText,
                                      minutesInput: EditText,
                                      secondsInput: EditText,
                                      millisInput: EditText): Long {
        val hoursStr = hoursInput.text.toString()
        val minutesStr = minutesInput.text.toString()
        val secondsStr = secondsInput.text.toString()
        val millisStr = millisInput.text.toString()
        val hours = if (hoursStr.isEmpty()) 0 else Integer.parseInt(hoursStr)
        val minutes = if (minutesStr.isEmpty()) 0 else Integer.parseInt(minutesStr)
        val seconds = if (secondsStr.isEmpty()) 0 else Integer.parseInt(secondsStr)
        val millis = if (millisStr.isEmpty()) 0 else Integer.parseInt(millisStr)
        return (1000 * 60 * 60 * hours + 1000 * 60 * minutes + 1000 * seconds + millis).toLong()
    }
}
