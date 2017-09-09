package il.ronmad.speedruntimer;

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
}