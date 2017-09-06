package com.example.ronmad.speedruntimer;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Game implements Serializable {

    private String name;
    private Map<String, Long> categories;
    private int lastSelectedCategoryPosition;

    public Game(String name) {
        this.name = name;
        this.categories = new LinkedHashMap<>();
        this.lastSelectedCategoryPosition = 0;

    }

    public String getName() {
        return name;
    }

    Map<String, Long> getCategories() {
        return categories;
    }

    public int getLastSelectedCategoryPosition() {
        return lastSelectedCategoryPosition;
    }

    public void setLastSelectedCategoryPosition(int lastSelectedCategoryPosition) {
        this.lastSelectedCategoryPosition = lastSelectedCategoryPosition;
    }

    public long getBestTime(String category) {
        return categories.get(category);
    }

    public void setBestTime(String category, long time) {
        categories.put(category, time);
    }

    public String getFormattedBestTime(String category) {
        return hasCategory(category) ? new SimpleDateFormat(
                (getBestTime(category) / (1000*3600) > 0 ? "H:mm:ss.SS" : "m:ss.SS"),
                Locale.getDefault())
                .format(getBestTime(category))
                : "";
    }

    public Long addCategory(String category) {
        return categories.putIfAbsent(category, 0L);
    }

    public void removeCategory(String category) {
        categories.remove(category);
    }

    public boolean hasCategory(String category) {
        return categories.containsKey(category);
    }

    public boolean isEmpty() {
        return categories.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Game && ((Game)obj).name.equals(this.name)
                || obj instanceof String && obj.equals(this.name);
    }

    public static String getFormattedBestTime(long time) {
        return new SimpleDateFormat(
                (time / (1000*3600) > 0 ? "H:mm:ss.SS" : "m:ss.SS"),
                Locale.getDefault())
                .format(time);
    }
}
