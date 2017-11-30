package il.ronmad.speedruntimer;

public class Category {

    String name;
    long bestTime;
    int runCount;

    public Category(String name) {
        this.name = name;
        bestTime = 0L;
        runCount = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Category category = (Category) o;

        return name.equals(category.name);
    }
}
