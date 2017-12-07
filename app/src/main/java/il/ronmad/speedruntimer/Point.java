package il.ronmad.speedruntimer;

import io.realm.RealmObject;

public class Point extends RealmObject {

    public int x;
    public int y;

    void set(int x, int y) {
        getRealm().executeTransaction(realm -> {
            this.x = x;
            this.y = y;
        });
    }
}
