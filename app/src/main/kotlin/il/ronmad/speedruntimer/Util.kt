package il.ronmad.speedruntimer

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
}
