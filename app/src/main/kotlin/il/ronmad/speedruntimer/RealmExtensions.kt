package il.ronmad.speedruntimer

import io.realm.Case
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.oneOf
import io.realm.kotlin.where

fun Game.setGameName(newName: String) = realm.executeTransaction { this.name = newName }

fun Game.getCategory(name: String) =
        this.categories.where().equalTo("name", name).findFirst()

fun Game.addCategory(name: String) = realm.executeTransaction {
    val category = realm.createObject<Category>()
    category.name = name
    this.categories.add(category)
}

fun Game.categoryExists(name: String) =
        this.categories.where().equalTo("name", name, Case.INSENSITIVE).count() > 0

fun Game.removeCategories(toRemove: List<Category>) {
    val names = toRemove.map { it.name as? String }.toTypedArray()
    realm.executeTransaction {
        this.categories.where()
                .oneOf("name", names)
                .findAll()
                .deleteAllFromRealm()
    }
}

fun Game.getPosition(): Point {
    if (this.timerPosition == null) {
        realm.executeTransaction { this.timerPosition = realm.createObject() }
    }
    return this.timerPosition!!
}

fun Category.incrementRunCount() = realm.executeTransaction { this.runCount++ }

fun Category.setData(bestTime: Long, runCount: Int, name: String = this.name) = realm.executeTransaction {
    this.name = name
    this.bestTime = bestTime
    this.runCount = runCount
}

fun Point.set(x: Int, y: Int) = realm.executeTransaction {
    this.x = x
    this.y = y
}

fun Realm.addGame(gameName: String) = this.executeTransaction {
    val game = this.createObject<Game>()
    game.name = gameName
}

fun Realm.gameExists(name: String) =
        this.where<Game>().equalTo("name", name, Case.INSENSITIVE).count() > 0

fun Realm.removeGames(toRemove: List<Game>) {
    val names = toRemove.map { it.name as? String }.toTypedArray()
    this.executeTransaction {
        this.where<Game>()
                .oneOf("name", names)
                .findAll()
                .deleteAllFromRealm()
    }
}
