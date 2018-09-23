package il.ronmad.speedruntimer

import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.WindowManager
import il.ronmad.speedruntimer.adapters.SplitPositionSpinnerAdapter
import il.ronmad.speedruntimer.realm.*
import io.realm.Realm
import kotlinx.android.synthetic.main.edit_category_dialog.view.*
import kotlinx.android.synthetic.main.edit_split_dialog.view.*
import kotlinx.android.synthetic.main.import_splits_dialog.view.*
import kotlinx.android.synthetic.main.new_category_dialog.view.*
import kotlinx.android.synthetic.main.new_game_dialog.view.*
import kotlinx.android.synthetic.main.new_split_dialog.view.*

object Dialogs {

    internal fun newGameDialog(context: Context, realm: Realm,
                               callback: (String) -> Unit): AlertDialog {
        val dialogView = View.inflate(context, R.layout.new_game_dialog, null)
        val gameNameInput = dialogView.newGameNameInput
        return AlertDialog.Builder(context)
                .setTitle("New game")
                .setView(dialogView)
                .setPositiveButton(R.string.create, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .apply {
                    setOnShowListener { _ ->
                        val createButton = getButton(DialogInterface.BUTTON_POSITIVE)
                        createButton.setOnClickListener {
                            if (!gameNameInput.isValidForGame(realm)) {
                                gameNameInput.requestFocus()
                            } else {
                                callback(gameNameInput.text.toString())
                                dismiss()
                            }
                        }
                    }
                }
    }

    internal fun newCategoryDialog(context: Context, game: Game,
                                   callback: (String) -> Unit): AlertDialog {
        val dialogView = View.inflate(context, R.layout.new_category_dialog, null)
        val categoryNameInput = dialogView.newCategoryInput
        categoryNameInput.setCategories(game.name)
        return AlertDialog.Builder(context)
                .setTitle("New category")
                .setView(dialogView)
                .setPositiveButton(R.string.create, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .apply {
                    setOnShowListener { _ ->
                        val createButton = getButton(DialogInterface.BUTTON_POSITIVE)
                        createButton.setOnClickListener {
                            if (!categoryNameInput.isValidForCategory(game)) {
                                categoryNameInput.requestFocus()
                            } else {
                                callback(categoryNameInput.text.toString())
                                dismiss()
                            }
                        }
                    }
                }
    }

    fun newSplitDialog(context: Context, category: Category,
                       callback: (String, Int) -> Unit): AlertDialog {
        val dialogView = View.inflate(context, R.layout.new_split_dialog, null)
        val splitNameInput = dialogView.newSplitInput
        val splitPositionSpinner = dialogView.positionSpinner
        splitPositionSpinner.adapter = SplitPositionSpinnerAdapter(context,
                category.splits.size + 1)
        splitPositionSpinner.setSelection(splitPositionSpinner.count - 1)
        return AlertDialog.Builder(context)
                .setTitle("New split")
                .setView(dialogView)
                .setPositiveButton(R.string.create, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .apply {
                    setOnShowListener { _ ->
                        val createButton = getButton(DialogInterface.BUTTON_POSITIVE)
                        createButton.setOnClickListener {
                            if (!splitNameInput.isValidForSplit(category)) {
                                splitNameInput.requestFocus()
                            } else {
                                val position = splitPositionSpinner.selectedItem as Int - 1
                                callback(splitNameInput.text.toString(), position)
                                dismiss()
                            }
                        }
                    }
                }
    }

    internal fun editGameDialog(context: Context, realm: Realm, game: Game,
                                callback: (String) -> Unit): AlertDialog {
        val dialogView = View.inflate(context, R.layout.new_game_dialog, null)
        val gameNameInput = dialogView.newGameNameInput
        gameNameInput.setText(game.name)
        gameNameInput.setSelection(gameNameInput.text.length)
        return AlertDialog.Builder(context)
                .setTitle("Edit name")
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .apply {
                    setOnShowListener { _ ->
                        val createButton = getButton(DialogInterface.BUTTON_POSITIVE)
                        createButton.setOnClickListener {
                            val newName = gameNameInput.text.toString()
                            if (newName == game.name) {
                                dismiss()
                                return@setOnClickListener
                            }
                            if (gameNameInput.isValidForGame(realm)) {
                                callback(newName)
                                dismiss()
                            }
                        }
                    }
                }
    }

    internal fun editCategoryDialog(context: Context, category: Category,
                                    callback: (String, Long, Int) -> Unit): AlertDialog {
        val dialogView = View.inflate(context, R.layout.edit_category_dialog, null)
        dialogView.categoryName.setText(category.name)
        dialogView.categoryName.setSelection(dialogView.categoryName.text.length)
        if (category.bestTime > 0) {
            dialogView.setEditTextsFromTime(category.bestTime)
        }
        dialogView.runCount.setText(category.runCount.toString())
        return AlertDialog.Builder(context)
                .setTitle("Edit category")
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.pb_clear, null)
                .setNeutralButton(android.R.string.cancel, null)
                .create()
                .apply {
                    setOnShowListener { _ ->
                        val clearButton = getButton(DialogInterface.BUTTON_NEGATIVE)
                        val saveButton = getButton(DialogInterface.BUTTON_POSITIVE)
                        clearButton.setOnClickListener {
                            dialogView.setEditTextsFromTime(0L)
                            dialogView.runCount.setText("0")
                        }
                        saveButton.setOnClickListener {
                            val newName = dialogView.categoryName.text.toString()
                            if (newName == category.name
                                    || dialogView.categoryName.isValidForCategory(category.getGame())) {
                                val newTime = dialogView.getTimeFromEditTexts()
                                val newRunCountStr = dialogView.runCount.text.toString()
                                val newRunCount = if (newRunCountStr.isEmpty()) 0 else Integer.parseInt(newRunCountStr)
                                callback(newName, newTime, newRunCount)
                                dismiss()
                            }
                        }
                    }
                }
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
                split.getCategory().splits.size)
        splitPositionSpinner.setSelection(split.getPosition())
        splitNameInput.setText(split.name)
        if (split.pbTime > 0) {
            pbSegmentTimeInput.setEditTextsFromTime(split.pbTime)
        }
        if (split.bestTime > 0) {
            bestSegmentTimeInput.setEditTextsFromTime(split.bestTime)
        }
        return AlertDialog.Builder(context)
                .setTitle("Edit split")
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.pb_clear, null)
                .setNeutralButton(android.R.string.cancel, null)
                .create()
                .apply {
                    setOnShowListener { _ ->
                        val clearButton = getButton(DialogInterface.BUTTON_NEGATIVE)
                        val saveButton = getButton(DialogInterface.BUTTON_POSITIVE)
                        clearButton.setOnClickListener {
                            pbSegmentTimeInput.setEditTextsFromTime(0L)
                            bestSegmentTimeInput.setEditTextsFromTime(0L)
                        }
                        saveButton.setOnClickListener {
                            val newName = splitNameInput.text.toString()
                            if (newName == split.name || splitNameInput.isValidForSplit(split.getCategory())) {
                                val newPBSegmentTime = pbSegmentTimeInput.getTimeFromEditTexts()
                                val newBestSegmentTime = bestSegmentTimeInput.getTimeFromEditTexts()
                                val position = splitPositionSpinner.selectedItem as Int - 1
                                callback(newName, newPBSegmentTime, newBestSegmentTime, position)
                                dismiss()
                            }
                        }
                    }
                }
    }

    internal fun timerActiveDialog(context: Context, callback: () -> Unit): AlertDialog {
        return AlertDialog.Builder(context)
                .setMessage("Timer is active. Close anyway?")
                .setPositiveButton(R.string.close) { _, _ -> callback() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> context.minimizeApp() }
                .create()
                .apply {
                    window?.setType(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
                }
    }

    internal fun closeTimerOnResumeDialog(context: Context, callback: () -> Unit): AlertDialog {
        return AlertDialog.Builder(context)
                .setMessage("Timer must be closed in order to use the app.")
                .setPositiveButton(R.string.close) { _, _ -> callback() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> context.minimizeApp() }
                .setCancelable(false)
                .create()
    }

    internal fun deleteCategoryDialog(context: Context, category: Category,
                                      callback: () -> Unit): AlertDialog {
        return AlertDialog.Builder(context)
                .setTitle("Delete ${category.gameName} ${category.name}?")
                .setMessage("Your PB of ${category.bestTime.getFormattedTime()} and splits will be lost.")
                .setPositiveButton(R.string.delete) { _, _ -> callback() }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
    }

    internal fun deleteCategoriesDialog(context: Context, callback: () -> Unit): AlertDialog {
        return AlertDialog.Builder(context)
                .setTitle("Delete selected categories?")
                .setMessage("Your PBs and splits will be lost.")
                .setPositiveButton(R.string.delete) { _, _ -> callback() }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
    }

    internal fun addInstalledGamesDialog(context: Context, realm: Realm, gameNames: List<String>,
                                         callback: () -> Unit): AlertDialog {
        val checked = BooleanArray(gameNames.size)
        return AlertDialog.Builder(context)
                .setTitle("Select games")
                .setPositiveButton(R.string.add) { _, _ ->
                    gameNames.filterIndexed { index, _ -> checked[index] }
                            .forEach { realm.addGame(it) }
                    if (checked.any { it }) callback()
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
                .setMessage("All categories, PBs and splits associated with the games will be lost.")
                .setPositiveButton(R.string.delete) { _, _ -> callback() }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
    }

    internal fun importSplitsDialog(context: Context, callback: (String) -> Unit): AlertDialog {
        val dialogView = View.inflate(context, R.layout.import_splits_dialog, null)
        val runIdInput = dialogView.runIdInput
        return AlertDialog.Builder(context)
                .setTitle("Import Splits from splits.io")
                .setView(dialogView)
                .setPositiveButton(R.string.do_import, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .apply {
                    setOnShowListener { _ ->
                        val importButton = getButton(DialogInterface.BUTTON_POSITIVE)
                        importButton.setOnClickListener {
                            if (runIdInput.text.isNullOrEmpty()) {
                                runIdInput.error = "Please enter a run ID"
                                runIdInput.requestFocus()
                                return@setOnClickListener
                            }
                            callback(runIdInput.text.toString())
                            dismiss()
                        }
                    }
                }
    }
}
