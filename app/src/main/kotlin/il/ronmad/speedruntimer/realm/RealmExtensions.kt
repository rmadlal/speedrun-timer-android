package il.ronmad.speedruntimer.realm

import il.ronmad.speedruntimer.Comparison
import il.ronmad.speedruntimer.sumBy
import io.realm.Case
import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmSchema
import io.realm.kotlin.createObject
import io.realm.kotlin.oneOf
import io.realm.kotlin.where

fun Realm.addGame(gameName: String): Game {
    var game: Game? = null
    this.executeTransaction {
        game = this.createObject(getNextId<Game>())
        game!!.name = gameName
        game!!.timerPosition = this.createObject(getNextId<Point>())
    }
    return game!!
}

fun Realm.getGameById(id: Long) =
        this.where<Game>().equalTo("id", id).findFirst()

fun Realm.getGameByName(name: String) =
        this.where<Game>().equalTo("name", name, Case.INSENSITIVE).findFirst()

fun Realm.getGames(ids: Collection<Long>) =
        this.where<Game>().oneOf("id", ids.toTypedArray()).findAll()!!

fun Realm.getAllGames() =
        this.where<Game>().findAll()!!

fun Realm.gameExists(name: String) =
        this.where<Game>().equalTo("name", name, Case.INSENSITIVE).count() > 0

fun Realm.removeGames(toRemove: Collection<Long>) = executeTransaction {
    this.getGames(toRemove).deleteAllFromRealm()
}

fun Realm.getCategoryByName(gameName: String, categoryName: String) =
        this.where<Category>()
                .equalTo("game.name", gameName, Case.INSENSITIVE)
                .equalTo("name", categoryName, Case.INSENSITIVE).findFirst()

fun Game.setGameName(newName: String) = realm.executeTransaction { this.name = newName }

fun Game.addCategory(name: String): Category {
    var category: Category? = null
    realm.executeTransaction {
        category = realm.createObject<Category>(realm.getNextId<Category>())
        category!!.name = name
        category!!.gameName = this.name
        this.categories.add(category)
    }
    return category!!
}

fun Game.getCategoryById(id: Long) =
        this.categories.where().equalTo("id", id).findFirst()

fun Game.getCategoryByName(name: String) =
        this.categories.where().equalTo("name", name, Case.INSENSITIVE).findFirst()

fun Game.getCategories(ids: Collection<Long>) =
        this.categories.where().oneOf("id", ids.toTypedArray()).findAll()!!

fun Game.categoryExists(name: String) =
        this.categories.where().equalTo("name", name, Case.INSENSITIVE).count() > 0

fun Game.removeCategories(toRemove: Collection<Long>) = realm.executeTransaction {
    this.getCategories(toRemove).deleteAllFromRealm()
}

fun Game.getPosition(): Point =
        this.timerPosition ?:
        run {
            realm.executeTransaction {
                this.timerPosition = realm.createObject(realm.getNextId<Point>()) }
            this.timerPosition!! }

fun Category.getGame() = this.game!!.first()!!

fun Category.incrementRunCount() = realm.executeTransaction { this.runCount++ }

fun Category.updateData(name: String = this.name,
                        bestTime: Long = this.bestTime,
                        runCount: Int = this.runCount) = realm.executeTransaction {
    this.name = name
    this.bestTime = bestTime
    this.runCount = runCount
}

fun Category.getSplitById(id: Long) =
        this.splits.where().equalTo("id", id).findFirst()

fun Category.getSplits(ids: Collection<Long>) =
        this.splits.where().oneOf("id", ids.toTypedArray()).findAll()!!

fun Category.addSplit(name: String, position: Int = this.splits.size): Split {
    var split: Split? = null
    realm.executeTransaction {
        split = realm.createObject<Split>(realm.getNextId<Split>())
        split!!.name = name
        this.splits.add(position, split)
    }
    return split!!
}

fun Category.splitExists(splitName: String) =
        this.splits.where().equalTo("name", splitName, Case.INSENSITIVE).count() > 0

fun Category.clearSplits(clearPBTimes: Boolean = true, clearBestTimes: Boolean = true) =
        realm.executeTransaction { _ ->
            this.splits.forEach {
                if (clearPBTimes) it.pbTime = 0L
                if (clearBestTimes) it.bestTime = 0L
            }
        }

fun Category.removeSplits(toRemove: Collection<Long>) = realm.executeTransaction {
    this.getSplits(toRemove).deleteAllFromRealm()
}

fun Category.setPBFromSplits() = updateData(bestTime = splits.sumBy { it.pbTime })

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
            splits.subList(0, splits.indexOf(this)).sumBy { it.pbTime } + this.pbTime
        Comparison.BEST_SEGMENTS ->
            splits.subList(0, splits.indexOf(this)).sumBy { it.bestTime } + this.bestTime
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

inline fun <reified T : RealmObject> Realm.getNextId() =
        (this.where<T>().max("id")?.toLong() ?: 0L) + 1
