package il.ronmad.speedruntimer;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Game {

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

    public Long addCategory(String category) {
        Long v = categories.get(category);
        if (v == null) {
            v = categories.put(category, 0L);
        }
        return v;
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
        int hours = (int) time / (1000*3600);
        int remaining = (int)(time % (3600 * 1000));
        int minutes = remaining / (60 * 1000);
        remaining = remaining % (60 * 1000);
        int seconds = remaining / 1000;
        int millis = (remaining % 1000) / 10;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d.%02d", hours, minutes, seconds, millis);
        }
        if (minutes > 0) {
            return String.format(Locale.getDefault(), "%d:%02d.%02d", minutes, seconds, millis);

        }
        return String.format(Locale.getDefault(), "%d.%02d", seconds, millis);
    }
}
