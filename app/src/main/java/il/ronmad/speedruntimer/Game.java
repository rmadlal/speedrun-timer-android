package il.ronmad.speedruntimer;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Required;

public class Game extends RealmObject {

    @Required
    public String name;
    public RealmList<Category> categories;
    public Point timerPosition;

    public void setName(String name) {
        getRealm().executeTransaction(realm -> this.name = name);
    }

    public Point getTimerPosition() {
        if (timerPosition == null) {
            getRealm().executeTransaction(realm ->
                    timerPosition = realm.createObject(Point.class));
        }
        return timerPosition;
    }

    Category getCategory(String name) {
        return categories.where().equalTo("name", name).findFirst();
    }

    void addCategory(String name) {
        getRealm().executeTransaction(realm -> {
            Category category = realm.createObject(Category.class);
            category.name = name;
            categories.add(category);
        });
    }

    boolean categoryExists(String name) {
        return categories.where().equalTo("name", name).count() > 0;
    }

    void removeCategories(Category[] toRemove) {
        String[] names = new String[toRemove.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = toRemove[i].name;
        }
        getRealm().executeTransaction(realm ->
                categories.where()
                          .in("name", names)
                          .findAll()
                          .deleteAllFromRealm());
    }
}
