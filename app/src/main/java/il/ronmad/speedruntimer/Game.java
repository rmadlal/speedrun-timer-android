package il.ronmad.speedruntimer;

import java.util.ArrayList;
import java.util.List;

public class Game {

    String name;
    List<Category> categories;
    private Point timerPosition;

    public Game(String name) {
        this.name = name;
        this.categories = new ArrayList<>();
        this.timerPosition = new Point();
    }

    public Game(String name, List<Category> categories, Point timerPosition) {
        this.name = name;
        this.categories = categories;
        this.timerPosition = timerPosition;
    }

    void addCategory(String category) {
        categories.add(new Category(category));
    }

    Category getCategory(String category) {
        return categories.get(categories.indexOf(new Category(category)));
    }

    boolean hasCategory(String category) {
        return categories.contains(new Category(category));
    }

    Point getTimerPosition() {
        if (timerPosition == null) {
            timerPosition = new Point();
        }
        return timerPosition;
    }

    void setTimerPosition(int x, int y) {
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
