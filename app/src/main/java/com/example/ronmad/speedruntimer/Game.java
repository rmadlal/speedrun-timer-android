package com.example.ronmad.speedruntimer;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Game implements Serializable {
    public String name;
    public long bestTime;

    public Game(String name, long bestTime) {
        this.name = name;
        this.bestTime = bestTime;
    }

    public String getFormattedBestTime() {
        return new SimpleDateFormat(
                (bestTime / (1000*3600) > 0 ? "H:mm:ss.SS" : "m:ss.SS"),
                Locale.getDefault())
                .format(bestTime);
    }
}
