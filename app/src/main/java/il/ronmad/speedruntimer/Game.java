package il.ronmad.speedruntimer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Game {

    private String name;
    private Map<String, Long> categories;

    public Game(String name) {
        this.name = name;
        this.categories = new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Long> getCategories() {
        return categories;
    }

    public Set<String> getCategoryNames() {
        return categories.keySet();
    }

    public Collection<Long> getBestTimes() {
        return categories.values();
    }

    public long getBestTime(String category) {
        return categories.get(category);
    }

    public void setBestTime(String category, long time) {
        categories.put(category, time);
    }

    public void addCategory(String category) {
        categories.put(category, 0L);
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
        if (obj instanceof Game) {
            return ((Game)obj).name.equals(this.name);
        }
        return obj instanceof String && obj.equals(this.name);
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
