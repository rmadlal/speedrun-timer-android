package il.ronmad.speedruntimer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Util {

    public static Gson gson = new GsonBuilder().create();

    public static int[] getTimeUnits(long time) {
        int hours = (int) time / (1000*3600);
        int remaining = (int)(time % (3600 * 1000));
        int minutes = remaining / (60 * 1000);
        remaining = remaining % (60 * 1000);
        int seconds = remaining / 1000;
        int millis = remaining % 1000;
        return new int[]{hours, minutes, seconds, millis};
    }
}
