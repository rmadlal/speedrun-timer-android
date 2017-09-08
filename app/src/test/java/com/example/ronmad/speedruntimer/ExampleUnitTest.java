package com.example.ronmad.speedruntimer;

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
        String time = "1:04:51.420";
        String[] parts = time.split("[:.]");
        System.out.println(Arrays.toString(parts));
    }
}