package il.ronmad.speedruntimer;

import android.graphics.Point;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Game {

    private String name;
    private Map<String, Long> categories;
    private Point timerPosition;

    public Game(String name) {
        this.name = name;
        this.categories = new LinkedHashMap<>();
        this.timerPosition = new Point();
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

    public Point getTimerPosition() {
        if (timerPosition == null) {
            timerPosition = new Point(0, 0);
        }
        return timerPosition;
    }

    public void setTimerPosition(int x, int y) {
        timerPosition.set(x, y);
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
}
