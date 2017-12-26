package il.ronmad.speedruntimer

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.edit_category_dialog.view.*
import kotlinx.android.synthetic.main.edit_time_layout.view.*
import kotlinx.android.synthetic.main.new_category_dialog.view.*
import kotlinx.android.synthetic.main.new_game_dialog.view.*

object Dialogs {

    internal fun newGameDialog(context: Context, realm: Realm): AlertDialog {
        val dialogView = View.inflate(context, R.layout.new_game_dialog, null)
        val gameNameInput = dialogView.newGameNameInput
        val dialog = AlertDialog.Builder(context)
                .setTitle("New game")
                .setView(dialogView)
                .setPositiveButton(R.string.create, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        dialog.setOnShowListener {
            val createButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            createButton.setOnClickListener {
                if (gameNameInput.isValidForGame(realm)) {
                    gameNameInput.requestFocus()
                } else {
                    realm.addGame(gameNameInput.text.toString())
                    dialog.dismiss()
                }
            }
        }
        return dialog
    }

    internal fun newCategoryDialog(context: Context, game: Game): AlertDialog {
        val dialogView = View.inflate(context, R.layout.new_category_dialog, null)
        val categoryNameInput = dialogView.newCategoryInput
        categoryNameInput.setCategories(game.name)
        val dialog = AlertDialog.Builder(context)
                .setTitle("New category")
                .setView(dialogView)
                .setPositiveButton(R.string.create, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        dialog.setOnShowListener {
            val createButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            createButton.setOnClickListener {
                if (categoryNameInput.isValidForCategory(game)) {
                    categoryNameInput.requestFocus()
                } else {
                    game.addCategory(categoryNameInput.text.toString())
                    dialog.dismiss()
                }
            }
        }
        return dialog
    }

    internal fun editGameDialog(context: Context, realm: Realm, game: Game): AlertDialog {
        val dialogView = View.inflate(context, R.layout.new_game_dialog, null)
        val gameNameInput = dialogView.newGameNameInput
        val dialog = AlertDialog.Builder(context)
                .setTitle("Edit name")
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        dialog.setOnShowListener {
            val createButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            createButton.setOnClickListener {
                if (gameNameInput.isValidForGame(realm)) {
                    gameNameInput.requestFocus()
                } else {
                    game.setGameName(gameNameInput.text.toString())
                    dialog.dismiss()
                }
            }
        }
        return dialog
    }

    internal fun editCategoryDialog(categoryListFragment: CategoryListFragment, category: Category): AlertDialog {
        val dialogView = View.inflate(categoryListFragment.context, R.layout.edit_category_dialog, null)
        if (category.bestTime > 0) {
            category.bestTime.setEditTextsFromTime(
                    dialogView.hours,
                    dialogView.minutes,
                    dialogView.seconds,
                    dialogView.milliseconds)
        }
        dialogView.runCount.setText(category.runCount.toString())
        val dialog = AlertDialog.Builder(categoryListFragment.context!!)
                .setTitle("Edit ${category.name}")
                .setView(dialogView)
                .setPositiveButton(R.string.save) { _, _ ->
                    val newTime = Util.getTimeFromEditTexts(dialogView.hours,
                            dialogView.minutes,
                            dialogView.seconds,
                            dialogView.milliseconds)
                    val newRunCountStr = dialogView.runCount.text.toString()
                    val newRunCount = if (newRunCountStr.isEmpty()) 0 else Integer.parseInt(newRunCountStr)
                    categoryListFragment.editCategory(category, newTime, newRunCount)
                }
                .setNegativeButton(R.string.pb_clear, null)
                .setNeutralButton(android.R.string.cancel, null)
                .create()
        dialog.setOnShowListener {
            val clearButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            clearButton.setOnClickListener {
                dialogView.hours.setText("")
                dialogView.minutes.setText("")
                dialogView.seconds.setText("")
                dialogView.milliseconds.setText("")
                dialogView.runCount.setText("0")
            }
        }
        return dialog
    }

    internal fun timerActiveDialog(context: Context): AlertDialog {
        val dialog = AlertDialog.Builder(context)
                .setMessage("Timer is active. Close anyway?")
                .setPositiveButton(R.string.close) { _, _ ->
                    context.stopService(Intent(context, TimerService::class.java))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        dialog.window?.setType(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        return dialog
    }

    internal fun closeTimerOnResumeDialog(context: Context): AlertDialog {
        return AlertDialog.Builder(context)
                .setMessage("Timer must be closed in order to use the app.")
                .setPositiveButton(R.string.close) { _, _ ->
                    context.stopService(Intent(context, TimerService::class.java))
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    val homeIntent = Intent(Intent.ACTION_MAIN)
                    homeIntent.addCategory(Intent.CATEGORY_HOME)
                    context.startActivity(homeIntent)
                }
                .setCancelable(false)
                .create()
    }

    internal fun deleteCategoryDialog(context: Context, game: Game, toRemove: List<Category>): AlertDialog {
        val category = toRemove[0]
        return AlertDialog.Builder(context)
                .setTitle("Delete ${game.name} ${category.name}?")
                .setMessage("Your PB of ${category.bestTime.getFormattedTime()} will be lost.")
                .setPositiveButton(R.string.delete) { _, _ -> game.removeCategories(toRemove) }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
    }

    internal fun deleteCategoriesDialog(context: Context, game: Game, toRemove: List<Category>): AlertDialog {
        return AlertDialog.Builder(context)
                .setTitle("Delete selected categories?")
                .setMessage("Your PBs will be lost.")
                .setPositiveButton(R.string.delete) { _, _ -> game.removeCategories(toRemove) }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
    }

    internal fun addInstalledGamesDialog(context: Context, realm: Realm, gameNames: List<String>): AlertDialog {
        val checked = BooleanArray(gameNames.size)
        return AlertDialog.Builder(context)
                .setTitle("Select games")
                .setPositiveButton(R.string.add) { _, _ ->
                    gameNames.filterIndexed { index, _ -> checked[index] }
                             .forEach { realm.addGame(it) }
                    if (checked.any { it }) {
                        Snackbar.make((context as MainActivity).fabAdd, "Games added", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setMultiChoiceItems(gameNames.toTypedArray(), checked) {
                    _, i, b -> checked[i] = b
                }
                .create()
    }

    internal fun deleteGamesDialog(context: Context, realm: Realm, toRemove: List<Game>): AlertDialog {
        return AlertDialog.Builder(context)
                .setTitle("Delete selected games?")
                .setMessage("All categories and PBs associated with the games will be lost.")
                .setPositiveButton(R.string.delete) { _, _ -> realm.removeGames(toRemove) }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
    }
}
