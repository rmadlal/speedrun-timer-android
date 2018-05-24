package il.ronmad.speedruntimer

import io.realm.Case
import io.realm.Realm
import io.realm.RealmObject
import io.realm.kotlin.createObject
import io.realm.kotlin.oneOf
import io.realm.kotlin.where

fun Game.setGameName(newName: String) = realm.executeTransaction { this.name = newName }

fun Game.getCategory(name: String) =
        this.categories.where().equalTo("name", name).findFirst()

fun Game.addCategory(name: String) = realm.executeTransaction {
    val category = realm.createObject<Category>(realm.getNextId<Category>())
    category.name = name
    this.categories.add(category)
}

fun Game.categoryExists(name: String) =
        this.categories.where().equalTo("name", name, Case.INSENSITIVE).count() > 0

fun Game.removeCategories(toRemove: List<Category>) {
    val ids = toRemove.map { it.id as? Long }.toTypedArray()
    realm.executeTransaction {
        this.categories.where()
                .oneOf("id", ids)
                .findAll()
                .deleteAllFromRealm()
    }
}

fun Game.getPosition(): Point =
        this.timerPosition ?:
        run {
            realm.executeTransaction {
                this.timerPosition = realm.createObject(realm.getNextId<Point>()) }
            this.timerPosition!! }

operator fun Point.component1() = this.x

operator fun Point.component2() = this.y

fun Category.getGame() = this.game!!.first()!!

fun Category.incrementRunCount() = realm.executeTransaction { this.runCount++ }

fun Category.updateData(name: String = this.name,
                        bestTime: Long = this.bestTime,
                        runCount: Int = this.runCount) = realm.executeTransaction {
    this.name = name
    this.bestTime = bestTime
    this.runCount = runCount
}

fun Category.getSplit(splitName: String) =
        this.splits.where().equalTo("name", splitName).findFirst()

fun Category.addSplit(name: String, position: Int = this.splits.count()) = realm.executeTransaction {
    val split = realm.createObject<Split>(realm.getNextId<Split>())
    split.name = name
    this.splits.add(position, split)
}

fun Category.splitExists(splitName: String) =
        this.splits.where().equalTo("name", splitName, Case.INSENSITIVE).count() > 0

fun Category.clearSplits(clearPBTimes: Boolean = true, clearBestTimes: Boolean = true) =
        realm.executeTransaction {
            this.splits.forEach {
                if (clearPBTimes) it.pbTime = 0L
                if (clearBestTimes) it.bestTime = 0L
            }
        }

fun Category.removeSplits(toRemove: Collection<Long>) = realm.executeTransaction {
    this.splits.where()
            .oneOf("id", toRemove.toTypedArray())
            .findAll()
            .deleteAllFromRealm()
}

fun Category.setPBFromSplits() = updateData(bestTime = splits.map { it.pbTime }.sum())

fun Split.getCategory() = this.category!!.first()!!

fun Split.updateData(name: String = this.name,
                     pbTime: Long = this.pbTime,
                     bestTime: Long = this.bestTime) = realm.executeTransaction {
    this.name = name
    this.pbTime = pbTime
    this.bestTime = bestTime
}

fun Split.calculateSplitTime(comparison: Comparison = Comparison.PERSONAL_BEST): Long {
    val splits = this.getCategory().splits
    return when (comparison) {
        Comparison.PERSONAL_BEST ->
            splits.subList(0, splits.indexOf(this)).map { it.pbTime }.sum() + this.pbTime
        Comparison.BEST_SEGMENTS ->
            splits.subList(0, splits.indexOf(this)).map { it.bestTime }.sum() + this.bestTime
    }
}

fun Split.hasTime(comparison: Comparison = Comparison.PERSONAL_BEST): Boolean {
    return when (comparison) {
        Comparison.PERSONAL_BEST -> this.pbTime > 0L
        Comparison.BEST_SEGMENTS -> this.bestTime > 0L
    }
}

fun Split.moveToPosition(newPosition: Int) = realm.executeTransaction {
    val splits = getCategory().splits
    splits.move(splits.indexOf(this), newPosition)
}

fun Split.getPosition() = getCategory().splits.indexOf(this)

fun Point.set(x: Int, y: Int) = realm.executeTransaction {
    this.x = x
    this.y = y
}

fun Realm.addGame(gameName: String) = this.executeTransaction {
    val game = this.createObject<Game>(getNextId<Game>())
    game.name = gameName
    game.timerPosition = this.createObject(getNextId<Point>())
}

fun Realm.gameExists(name: String) =
        this.where<Game>().equalTo("name", name, Case.INSENSITIVE).count() > 0

fun Realm.removeGames(toRemove: List<Game>) {
    val ids = toRemove.map { it.id as? Long }.toTypedArray()
    this.executeTransaction {
        this.where<Game>()
                .oneOf("id", ids)
                .findAll()
                .deleteAllFromRealm()
    }
}

inline fun <reified T : RealmObject> Realm.getNextId() =
        (this.where<T>().max("id")?.toLong() ?: 0L) + 1
