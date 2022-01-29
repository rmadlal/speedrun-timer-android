package il.ronmad.speedruntimer.realm

import il.ronmad.speedruntimer.Comparison
import il.ronmad.speedruntimer.sumByLong
import il.ronmad.speedruntimer.web.SplitsIO
import io.realm.Case
import io.realm.Realm
import io.realm.RealmObject
import io.realm.kotlin.createObject
import io.realm.kotlin.oneOf
import io.realm.kotlin.where

fun Realm.addGame(gameName: String): Game {
    var game: Game? = null
    executeTransaction {
        game = createObject(getNextId<Game>())
        game!!.name = gameName
        game!!.timerPosition = createObject(getNextId<Point>())
    }
    return game!!
}

fun Realm.getGameById(id: Long) = where<Game>().equalTo("id", id).findFirst()

fun Realm.getGameByName(name: String) =
    where<Game>().equalTo("name", name, Case.INSENSITIVE).findFirst()

fun Realm.getGames(ids: Collection<Long>) =
    where<Game>().oneOf("id", ids.toTypedArray()).findAll()!!

fun Realm.getAllGames() = where<Game>().findAll()!!

fun Realm.gameExists(name: String) =
    where<Game>().equalTo("name", name, Case.INSENSITIVE).count() > 0

fun Realm.removeGames(toRemove: Collection<Long>) = executeTransaction {
    getGames(toRemove).deleteAllFromRealm()
}

fun Realm.getCategoryByName(gameName: String, categoryName: String) =
    where<Category>()
        .equalTo("game.name", gameName, Case.INSENSITIVE)
        .equalTo("name", categoryName, Case.INSENSITIVE).findFirst()

fun Game.setGameName(newName: String) = realm.executeTransaction { name = newName }

fun Game.addCategory(categoryName: String): Category {
    var category: Category? = null
    realm.executeTransaction {
        category = realm.createObject(realm.getNextId<Category>())
        category!!.name = categoryName
        category!!.gameName = name
        categories.add(category)
    }
    return category!!
}

fun Game.getCategoryById(id: Long) =
    categories.where().equalTo("id", id).findFirst()

fun Game.getCategoryByName(name: String) =
    categories.where().equalTo("name", name, Case.INSENSITIVE).findFirst()

fun Game.getCategories(ids: Collection<Long>) =
    categories.where().oneOf("id", ids.toTypedArray()).findAll()!!

fun Game.categoryExists(name: String) =
    categories.where().equalTo("name", name, Case.INSENSITIVE).count() > 0

fun Game.removeCategories(toRemove: Collection<Long>) = realm.executeTransaction {
    getCategories(toRemove).deleteAllFromRealm()
}

fun Game.getPosition(): Point =
    timerPosition ?: run {
        realm.executeTransaction {
            timerPosition = realm.createObject(realm.getNextId<Point>())
        }
        timerPosition!!
    }

fun Category.getGame() = game!!.first()!!

fun Category.incrementRunCount() = realm.executeTransaction { runCount++ }

fun Category.updateData(
    name: String = this.name,
    bestTime: Long = this.bestTime,
    runCount: Int = this.runCount
) = realm.executeTransaction {
    this.name = name
    this.bestTime = bestTime
    this.runCount = runCount
}

fun Category.getSplitById(id: Long) =
    splits.where().equalTo("id", id).findFirst()

fun Category.getSplits(ids: Collection<Long>) =
    splits.where().oneOf("id", ids.toTypedArray()).findAll()!!

fun Category.addSplit(name: String, position: Int = splits.size): Split {
    var split: Split? = null
    realm.executeTransaction {
        split = realm.createObject(realm.getNextId<Split>())
        split!!.name = name
        splits.add(position, split)
    }
    return split!!
}

fun Category.splitExists(splitName: String) =
    splits.where().equalTo("name", splitName, Case.INSENSITIVE).count() > 0

fun Category.clearSplits(clearPBTimes: Boolean = true, clearBestTimes: Boolean = true) =
    realm.executeTransaction {
        splits.forEach { split ->
            if (clearPBTimes) split.pbTime = 0L
            if (clearBestTimes) split.bestTime = 0L
        }
    }

fun Category.removeSplits(toRemove: Collection<Long>) = realm.executeTransaction {
    getSplits(toRemove).deleteAllFromRealm()
}

fun Category.setPBFromSplits() = updateData(bestTime = splits.sumByLong { it.pbTime })

fun Category.calculateSob() = splits.sumByLong { it.bestTime }

fun Category.toRun(): SplitsIO.Run =
    SplitsIO.Run(
        gameName,
        name,
        runCount,
        splits.map { it.toSegment() })

fun Split.getCategory() = category!!.first()!!

fun Split.updateData(
    name: String = this.name,
    pbTime: Long = this.pbTime,
    bestTime: Long = this.bestTime
) = realm.executeTransaction {
    this.name = name
    this.pbTime = pbTime
    this.bestTime = bestTime
}

fun Split.calculateSplitTime(comparison: Comparison = Comparison.PERSONAL_BEST): Long {
    val splits = getCategory().splits
    return when (comparison) {
        Comparison.PERSONAL_BEST ->
            splits.subList(0, splits.indexOf(this)).sumByLong { it.pbTime } + pbTime
        Comparison.BEST_SEGMENTS ->
            splits.subList(0, splits.indexOf(this)).sumByLong { it.bestTime } + bestTime
    }
}

fun Split.hasTime(comparison: Comparison = Comparison.PERSONAL_BEST): Boolean {
    return when (comparison) {
        Comparison.PERSONAL_BEST -> pbTime > 0L
        Comparison.BEST_SEGMENTS -> bestTime > 0L
    }
}

fun Split.moveToPosition(newPosition: Int) = realm.executeTransaction {
    val splits = getCategory().splits
    splits.move(splits.indexOf(this), newPosition)
}

fun Split.getPosition() = getCategory().splits.indexOf(this)

fun Split.toSegment(): SplitsIO.Segment = SplitsIO.Segment(name, pbTime, bestTime)

fun Point.set(x: Int, y: Int) = realm.executeTransaction {
    this.x = x
    this.y = y
}

inline fun <reified T : RealmObject> Realm.getNextId() =
    (where<T>().max("id")?.toLong() ?: 0L) + 1

inline fun <R> withRealm(block: Realm.() -> R): R {
    Realm.getDefaultInstance().use {
        return it.block()
    }
}
