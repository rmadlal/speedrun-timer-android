package il.ronmad.speedruntimer;

import android.graphics.Color;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

import java.util.Arrays;

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
    public void testFromJsonLegacy() throws Exception {
        Gson gson = new GsonBuilder().create();
        String fromJsonLegacy = "[{\"categories\":{\"Any%\":2180,\"100%\":1500},\"name\":\"Dudes\",\"timerPosition\":{\"x\":557,\"y\":1012}},{\"categories\":{},\"name\":\"Wh\",\"timerPosition\":{\"x\":0,\"y\":0}},{\"categories\":{\"Any%\":0,\"Low%\":550,\"Cat\":5000},\"name\":\"No\",\"timerPosition\":{\"x\":10,\"y\":20}}]";
        String fromJsonNew = "[{\"categories\":[{\"bestTime\":2180,\"name\":\"Any%\",\"runCount\":0},{\"bestTime\":1500,\"name\":\"100%\",\"runCount\":0}],\"name\":\"Dudes\",\"timerPosition\":{\"x\":557,\"y\":1012}},{\"categories\":[],\"name\":\"Wh\",\"timerPosition\":{\"x\":0,\"y\":0}},{\"categories\":[{\"bestTime\":0,\"name\":\"Any%\",\"runCount\":0},{\"bestTime\":550,\"name\":\"Low%\",\"runCount\":0},{\"bestTime\":5000,\"name\":\"Cat\",\"runCount\":0}],\"name\":\"No\",\"timerPosition\":{\"x\":10,\"y\":20}}]";
        String toJsonLegacy = gson.toJson(Util.fromJsonLegacy(fromJsonLegacy));
        String toJsonNew = gson.toJson(gson.fromJson(fromJsonNew, Game[].class));
        assertEquals(toJsonLegacy, toJsonNew);
    }
}