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
import kotlinx.android.synthetic.main.edit_split_dialog.view.*
import kotlinx.android.synthetic.main.new_category_dialog.view.*
import kotlinx.android.synthetic.main.new_game_dialog.view.*
import kotlinx.android.synthetic.main.new_split_dialog.view.*

object Dialogs : TimeExtensions {

    internal fun newGameDialog(context: Context, realm: Realm,
                               callback: (String) -> Unit): AlertDialog {
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
                if (!gameNameInput.isValidForGame(realm)) {
                    gameNameInput.requestFocus()
                } else {
                    callback(gameNameInput.text.toString())
                    dialog.dismiss()
                }
            }
        }
        return dialog
    }

    internal fun newCategoryDialog(context: Context, game: Game,
                                   callback: (String) -> Unit): AlertDialog {
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
                if (!categoryNameInput.isValidForCategory(game)) {
                    categoryNameInput.requestFocus()
                } else {
                    callback(categoryNameInput.text.toString())
                    dialog.dismiss()
                }
            }
        }
        return dialog
    }

    fun newSplitDialog(context: Context, category: Category,
                       callback: (String, Int) -> Unit): AlertDialog {
        val dialogView = View.inflate(context, R.layout.new_split_dialog, null)
        val splitNameInput = dialogView.newSplitInput
        val splitPositionSpinner = dialogView.positionSpinner
        splitPositionSpinner.adapter = SplitPositionSpinnerAdapter(context,
                category.splits.count() + 1)
        splitPositionSpinner.setSelection(splitPositionSpinner.count - 1)
        val dialog = AlertDialog.Builder(context)
                .setTitle("New split")
                .setView(dialogView)
                .setPositiveButton(R.string.create, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        dialog.setOnShowListener {
            val createButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            createButton.setOnClickListener {
                if (!splitNameInput.isValidForSplit(category)) {
                    splitNameInput.requestFocus()
                } else {
                    val position = splitPositionSpinner.selectedItem as Int - 1
                    callback(splitNameInput.text.toString(), position)
                    dialog.dismiss()
                }
            }
        }
        return dialog
    }

    internal fun editGameDialog(context: Context, realm: Realm, game: Game,
                                callback: (String) -> Unit): AlertDialog {
        val dialogView = View.inflate(context, R.layout.new_game_dialog, null)
        val gameNameInput = dialogView.newGameNameInput
        gameNameInput.setText(game.name)
        gameNameInput.setSelection(gameNameInput.text.length)
        val dialog = AlertDialog.Builder(context)
                .setTitle("Edit name")
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        dialog.setOnShowListener {
            val createButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            createButton.setOnClickListener {
                val newName = gameNameInput.text.toString()
                if (newName == game.name) {
                    dialog.dismiss()
                    return@setOnClickListener
                }
                if (!gameNameInput.isValidForGame(realm)) {
                    gameNameInput.requestFocus()
                } else {
                    callback(newName)
                    dialog.dismiss()
                }
            }
        }
        return dialog
    }

    internal fun editCategoryDialog(context: Context, category: Category,
                                    callback: (String, Long, Int) -> Unit): AlertDialog {
        val dialogView = View.inflate(context, R.layout.edit_category_dialog, null)
        dialogView.categoryName.setText(category.name)
        dialogView.categoryName.setSelection(dialogView.categoryName.text.length)
        if (category.bestTime > 0) {
            category.bestTime.setEditTextsFromTime(dialogView)
        }
        dialogView.runCount.setText(category.runCount.toString())
        val dialog = AlertDialog.Builder(context)
                .setTitle("Edit category")
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.pb_clear, null)
                .setNeutralButton(android.R.string.cancel, null)
                .create()
        dialog.setOnShowListener {
            val clearButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            val saveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            clearButton.setOnClickListener {
                0L.setEditTextsFromTime(dialogView)
                dialogView.runCount.setText("0")
            }
            saveButton.setOnClickListener {
                val newName = dialogView.categoryName.text.toString()
                if (newName != category.name
                        && !dialogView.categoryName.isValidForCategory(category.getGame())) {
                    dialogView.categoryName.requestFocus()
                    return@setOnClickListener
                }
                val newTime = dialogView.getTimeFromEditTexts()
                val newRunCountStr = dialogView.runCount.text.toString()
                val newRunCount = if (newRunCountStr.isEmpty()) 0 else Integer.parseInt(newRunCountStr)
                callback(newName, newTime, newRunCount)
                dialog.dismiss()
            }
        }
        return dialog
    }

    internal fun editSplitDialog(context: Context,
                                 split: Split,
                                 callback: (String, Long, Long, Int) -> Unit): AlertDialog {
        val dialogView = View.inflate(context, R.layout.edit_split_dialog, null)
        val splitNameInput = dialogView.nameInput
        val pbSegmentTimeInput = dialogView.editTimePB
        val bestSegmentTimeInput = dialogView.editTimeBest
        val splitPositionSpinner = dialogView.editPositionSpinner
        splitPositionSpinner.adapter = SplitPositionSpinnerAdapter(context,
                split.getCategory().splits.count())
        splitPositionSpinner.setSelection(split.getPosition())
        splitNameInput.setText(split.name)
        if (split.pbTime > 0) {
            split.pbTime.setEditTextsFromTime(pbSegmentTimeInput)
        }
        if (split.bestTime > 0) {
            split.bestTime.setEditTextsFromTime(bestSegmentTimeInput)
        }
        val dialog = AlertDialog.Builder(context)
                .setTitle("Edit split")
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.pb_clear, null)
                .setNeutralButton(android.R.string.cancel, null)
                .create()
        dialog.setOnShowListener {
            val clearButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            val saveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            clearButton.setOnClickListener {
                0L.setEditTextsFromTime(pbSegmentTimeInput)
                0L.setEditTextsFromTime(bestSegmentTimeInput)
            }
            saveButton.setOnClickListener {
                val newName = splitNameInput.text.toString()
                if (newName != split.name && !splitNameInput.isValidForSplit(split.getCategory())) {
                    splitNameInput.requestFocus()
                    return@setOnClickListener
                }
                val newPBSegmentTime = pbSegmentTimeInput.getTimeFromEditTexts()
                val newBestSegmentTime = bestSegmentTimeInput.getTimeFromEditTexts()
                val position = splitPositionSpinner.selectedItem as Int - 1
                callback(newName, newPBSegmentTime, newBestSegmentTime, position)
                dialog.dismiss()
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

    internal fun deleteCategoryDialog(context: Context, category: Category,
                                      callback: () -> Unit): AlertDialog {
        return AlertDialog.Builder(context)
                .setTitle("Delete ${category.getGame().name} ${category.name}?")
                .setMessage("Your PB of ${category.bestTime.getFormattedTime()} will be lost.")
                .setPositiveButton(R.string.delete) { _, _ -> callback() }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
    }

    internal fun deleteCategoriesDialog(context: Context, callback: () -> Unit): AlertDialog {
        return AlertDialog.Builder(context)
                .setTitle("Delete selected categories?")
                .setMessage("Your PBs will be lost.")
                .setPositiveButton(R.string.delete) { _, _ -> callback() }
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

    internal fun deleteGamesDialog(context: Context, callback: () -> Unit): AlertDialog {
        return AlertDialog.Builder(context)
                .setTitle("Delete selected games?")
                .setMessage("All categories and PBs associated with the games will be lost.")
                .setPositiveButton(R.string.delete) { _, _ -> callback() }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
    }
}
