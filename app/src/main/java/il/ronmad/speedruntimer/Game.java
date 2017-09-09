package il.ronmad.speedruntimer;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Game implements Serializable {

    static final long serialVersionUID = Game.class.hashCode();

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
        int hours = (int) time / (1000*3600);
        if (hours > 0) {
            return hours + new SimpleDateFormat(":mm:ss.SS", Locale.getDefault())
                    .format(time);
        }
        return new SimpleDateFormat("m:ss.SS", Locale.getDefault())
                .format(time);
    }
}
