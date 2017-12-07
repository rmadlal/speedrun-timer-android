package il.ronmad.speedruntimer;

import io.realm.RealmObject;
import io.realm.annotations.Required;

public class Category extends RealmObject {

    @Required
    public String name;
    public long bestTime;
    public int runCount;

    void incrementRunCount() {
        getRealm().executeTransaction(realm -> this.runCount++);
    }

    void setData(long bestTime, int runCount) {
        getRealm().executeTransaction(realm -> {
            this.bestTime = bestTime;
            this.runCount = runCount;
        });
    }
}
