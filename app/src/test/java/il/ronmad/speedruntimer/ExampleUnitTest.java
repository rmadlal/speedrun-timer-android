package il.ronmad.speedruntimer;

import android.graphics.Color;
import android.net.Uri;
import android.util.Xml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void testTimeSplit() throws Exception {
        long bestTime = 43 + 1000*27 + 1000*60*5;
        int hours = (int)(bestTime / (3600 * 1000));
        int remaining = (int)(bestTime % (3600 * 1000));
        int minutes = remaining / (60 * 1000);
        remaining = remaining % (60 * 1000);
        int seconds = remaining / 1000;
        int milliseconds = remaining % (1000);

        assertEquals(0, hours);
        assertEquals(5, minutes);
        assertEquals(27, seconds);
        assertEquals(43, milliseconds);
    }

    @Test
    public void testAlpha() throws Exception {
        System.out.println(Color.alpha(Color.argb(255, 255, 255, 255)));
    }

    @Test
    public void testSpeedrunComCategories() throws Exception {
        String srcApi = "https://www.speedrun.com/api/v1";
        String gameName = URLEncoder.encode("Monument Valley", "UTF-8");
        URL u = new URL(srcApi + "/games?name=" + gameName);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
        JsonObject gameData = json.get("data").getAsJsonArray().get(0).getAsJsonObject();
        String id = gameData.get("id").getAsString();

        u = new URL(srcApi + "/games/" + id + "/categories");
        conn = (HttpURLConnection) u.openConnection();
        reader = new InputStreamReader(conn.getInputStream());
        json = new JsonParser().parse(reader).getAsJsonObject();
        JsonArray categoriesData = json.get("data").getAsJsonArray();
        for (JsonElement categoryElement : categoriesData) {
            JsonObject categoryObject = categoryElement.getAsJsonObject();
            System.out.println(categoryObject.get("name").getAsString());
        }
    }
}