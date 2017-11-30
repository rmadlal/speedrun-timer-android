package il.ronmad.speedruntimer;

import android.widget.EditText;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Util {

    static List<Game> fromJsonLegacy(String json) {
        List<Game> games = new ArrayList<>();
        JsonArray gamesArray = new JsonParser().parse(json).getAsJsonArray();
        for (JsonElement gameElement : gamesArray) {
            JsonObject gameObject = gameElement.getAsJsonObject();
            String name = gameObject.get("name").getAsString();
            JsonObject categoriesObject = gameObject.get("categories").getAsJsonObject();
            Point timerPosition;
            try {
                JsonObject timerPositionObject = gameObject.get("timerPosition").getAsJsonObject();
                int x = timerPositionObject.get("x").getAsInt();
                int y = timerPositionObject.get("y").getAsInt();
                timerPosition = new Point(x, y);
            } catch (NullPointerException e) {
                timerPosition = new Point();
            }
            List<Category> categories = new ArrayList<>();
            for (String key : categoriesObject.keySet()) {
                Category category = new Category(key);
                category.bestTime = categoriesObject.get(key).getAsInt();
                categories.add(category);
            }
            games.add(new Game(name, categories, timerPosition));
        }
        return games;
    }

    static int[] getTimeUnits(long time) {
        int hours = (int) time / (1000*3600);
        int remaining = (int)(time % (3600 * 1000));
        int minutes = remaining / (60 * 1000);
        remaining = remaining % (60 * 1000);
        int seconds = remaining / 1000;
        int millis = remaining % 1000;
        return new int[]{hours, minutes, seconds, millis};
    }

    static String getFormattedTime(long time) {
        int[] units = Util.getTimeUnits(Math.abs(time));
        int hours = units[0], minutes = units[1], seconds = units[2], millis = units[3] / 10;
        String formattedTime;
        if (hours > 0) {
            formattedTime = String.format(Locale.getDefault(), "%d:%02d:%02d.%02d", hours, minutes, seconds, millis);
        } else if (minutes > 0) {
            formattedTime = String.format(Locale.getDefault(), "%d:%02d.%02d", minutes, seconds, millis);
        } else {
            formattedTime = String.format(Locale.getDefault(), "%d.%02d", seconds, millis);
        }
        if (time < 0) {
            formattedTime = "-" + formattedTime;
        }
        return formattedTime;
    }

    static void setEditTextsFromTime(long bestTime,
                                            EditText hoursInput,
                                            EditText minutesInput,
                                            EditText secondsInput,
                                            EditText millisInput) {
        int[] units = Util.getTimeUnits(bestTime);
        int hours = units[0], minutes = units[1], seconds = units[2], millis = units[3];
        hoursInput.setText(hours > 0 ? ""+hours : "");
        minutesInput.setText(minutes > 0 ? ""+minutes : "");
        secondsInput.setText(seconds > 0 ? ""+seconds : "");
        millisInput.setText(millis > 0 ? ""+millis : "");
    }

    static long getTimeFromEditTexts(EditText hoursInput,
                                            EditText minutesInput,
                                            EditText secondsInput,
                                            EditText millisInput) {
        String hoursStr = hoursInput.getText().toString();
        String minutesStr = minutesInput.getText().toString();
        String secondsStr = secondsInput.getText().toString();
        String millisStr = millisInput.getText().toString();
        int hours = hoursStr.isEmpty() ? 0 : Integer.parseInt(hoursStr);
        int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);
        int seconds = secondsStr.isEmpty() ? 0 : Integer.parseInt(secondsStr);
        int millis = millisStr.isEmpty() ? 0 : Integer.parseInt(millisStr);
        return 1000*60*60 * hours + 1000*60 * minutes + 1000 * seconds + millis;
    }
}
