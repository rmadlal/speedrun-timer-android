package il.ronmad.speedruntimer

import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.isItemChecked
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import il.ronmad.speedruntimer.adapters.SplitPositionSpinnerAdapter
import il.ronmad.speedruntimer.realm.*
import io.realm.Realm
import kotlinx.android.synthetic.main.edit_category_dialog.view.*
import kotlinx.android.synthetic.main.edit_split_dialog.view.*
import kotlinx.android.synthetic.main.new_category_dialog.view.*
import kotlinx.android.synthetic.main.new_split_dialog.view.*

object Dialogs {

    internal fun showNewGameDialog(context: Context, realm: Realm, callback: (String) -> Unit) {
        MaterialDialog(context).show {
            title(text = "New game")
            input(hintRes = R.string.hint_game_title)
            positiveButton(R.string.create) { dialog ->
                dialog.getInputField()?.let {
                    if (it.isValidForGame(realm)) {
                        callback(it.text.toString())
                        dialog.dismiss()
                    }
                }
            }
            noAutoDismiss()
        }
    }

    internal fun showNewCategoryDialog(context: Context, game: Game, callback: (String) -> Unit) {
        val dialogView = View.inflate(context, R.layout.new_category_dialog, null)
        val categoryNameInput = dialogView.newCategoryInput
        categoryNameInput.setCategories(game.name)
        AlertDialog.Builder(context)
                .setTitle("New category")
                .setView(dialogView)
                .setPositiveButton(R.string.create, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .apply {
                    setOnShowListener {
                        val createButton = getButton(DialogInterface.BUTTON_POSITIVE)
                        createButton.isEnabled = false
                        categoryNameInput.onTextChanged { text ->
                            createButton.isEnabled = !text.isNullOrBlank()
                        }
                        createButton.setOnClickListener {
                            if (categoryNameInput.isValidForCategory(game)) {
                                callback(categoryNameInput.text.toString())
                                dismiss()
                            }
                        }
                    }
                }
                .show()
    }

    fun showNewSplitDialog(context: Context, category: Category, callback: (String, Int) -> Unit) {
        MaterialDialog(context).show {
            title(text = "New split")
            customView(R.layout.new_split_dialog).also { dialog ->
                dialog.setActionButtonEnabled(WhichButton.POSITIVE, false)
                dialog.getCustomView()?.run {
                    newSplitInput.onTextChanged {
                        dialog.setActionButtonEnabled(WhichButton.POSITIVE, !it.isNullOrBlank())
                    }
                    positionSpinner.apply {
                        adapter = SplitPositionSpinnerAdapter(context,
                                category.splits.size + 1)
                        setSelection(count - 1)
                    }
                }
            }
            positiveButton(R.string.create) { dialog ->
                dialog.getCustomView()?.run {
                    if (newSplitInput.isValidForSplit(category)) {
                        val position = positionSpinner.selectedItem as Int - 1
                        callback(newSplitInput.text.toString(), position)
                        dialog.dismiss()
                    }
                }
            }
            noAutoDismiss()
        }
    }

    internal fun showEditGameDialog(context: Context, game: Game, callback: (String) -> Unit) {
        MaterialDialog(context).show {
            title(text = "Edit name")
            input(hintRes = R.string.hint_game_title, prefill = game.name)
            positiveButton(R.string.save) { dialog ->
                dialog.getInputField()?.let {
                    if (it.text.toString() == game.name) {
                        dialog.dismiss()
                        return@let
                    }
                    if (it.isValidForGame(game.realm)) {
                        callback(it.text.toString())
                        dialog.dismiss()
                    }
                }
            }
            noAutoDismiss()
        }
    }

    internal fun showEditCategoryDialog(
            context: Context,
            category: Category,
            callback: (String, Long, Int) -> Unit
    ) {
        MaterialDialog(context).show {
            title(text = "Edit category")
            customView(R.layout.edit_category_dialog).also { dialog ->
                dialog.getCustomView()?.run {
                    categoryName.apply {
                        setText(category.name)
                        setSelection(text.length)
                        onTextChanged {
                            dialog.setActionButtonEnabled(WhichButton.POSITIVE, !it.isNullOrBlank())
                        }
                    }
                    if (category.bestTime > 0) {
                        setEditTextsFromTime(category.bestTime)
                    }
                    runCount.setText(category.runCount.toString())
                }
            }
            positiveButton(R.string.save) { dialog ->
                dialog.getCustomView()?.run {
                    val newName = categoryName.text.toString()
                    if (newName == category.name
                            || categoryName.isValidForCategory(category.getGame())) {
                        val newTime = getTimeFromEditTexts()
                        val newRunCountStr = runCount.text.toString()
                        val newRunCount = if (newRunCountStr.isEmpty()) 0 else Integer.parseInt(newRunCountStr)
                        callback(newName, newTime, newRunCount)
                        dialog.dismiss()
                    }
                }
            }
            negativeButton(R.string.pb_clear) { dialog ->
                dialog.getCustomView()?.run {
                    setEditTextsFromTime(0L)
                    runCount.setText("0")
                }
            }
            noAutoDismiss()
        }
    }

    internal fun showEditSplitDialog(
            context: Context,
            split: Split,
            callback: (String, Long, Long, Int) -> Unit
    ) {
        MaterialDialog(context).show {
            title(text = "Edit split")
            customView(R.layout.edit_split_dialog).also { dialog ->
                dialog.getCustomView()?.run {
                    editPositionSpinner.apply {
                        adapter = SplitPositionSpinnerAdapter(context,
                                split.getCategory().splits.size)
                        setSelection(split.getPosition())
                    }
                    nameInput.apply {
                        setText(split.name)
                        setSelection(text.length)
                        onTextChanged {
                            dialog.setActionButtonEnabled(WhichButton.POSITIVE, !it.isNullOrBlank())
                        }
                    }
                    if (split.pbTime > 0) {
                        editTimePB.setEditTextsFromTime(split.pbTime)
                    }
                    if (split.bestTime > 0) {
                        editTimeBest.setEditTextsFromTime(split.bestTime)
                    }
                }
            }
            positiveButton(R.string.save) { dialog ->
                dialog.getCustomView()?.run {
                    val newName = nameInput.text.toString()
                    if (newName == split.name
                            || nameInput.isValidForSplit(split.getCategory())) {
                        val newPBSegmentTime = editTimePB.getTimeFromEditTexts()
                        val newBestSegmentTime = editTimeBest.getTimeFromEditTexts()
                        val position = editPositionSpinner.selectedItem as Int - 1
                        callback(newName, newPBSegmentTime, newBestSegmentTime, position)
                        dialog.dismiss()
                    }
                }

            }
            negativeButton(R.string.pb_clear) { dialog ->
                dialog.getCustomView()?.run {
                    editTimePB.setEditTextsFromTime(0L)
                    editTimeBest.setEditTextsFromTime(0L)
                }
            }
            noAutoDismiss()
        }
    }

    internal fun showTimerActiveDialog(context: Context, fromOnResume: Boolean, callback: () -> Unit) {
        MaterialDialog(context).show {
            message(R.string.dialog_timer_active)
            positiveButton(R.string.close) { callback() }
            negativeButton(android.R.string.cancel) {
                if (fromOnResume) {
                    context.minimizeApp()
                }
            }
            window?.setType(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
            if (fromOnResume) {
                cancelable(false)
                cancelOnTouchOutside(false)
            }
        }
    }

    internal fun showCloseTimerOnResumeDialog(context: Context, callback: () -> Unit) {
        MaterialDialog(context).show {
            message(R.string.dialog_close_timer_on_resume)
            positiveButton(R.string.close) { callback() }
            negativeButton(android.R.string.cancel) { context.minimizeApp() }
            cancelable(false)
            cancelOnTouchOutside(false)
        }
    }

    internal fun showDeleteCategoryDialog(
            context: Context,
            category: Category,
            callback: () -> Unit
    ) {
        MaterialDialog(context).show {
            title(text = "Delete ${category.gameName} ${category.name}?")
            message(text = "Your PB of ${category.bestTime.getFormattedTime()} and splits will be lost.")
            positiveButton(R.string.delete) { callback() }
            negativeButton(android.R.string.cancel)
        }
    }

    internal fun showDeleteCategoriesDialog(context: Context, callback: () -> Unit) {
        MaterialDialog(context).show {
            title(R.string.dialog_delete_categories)
            message(R.string.dialog_delete_categories_msg)
            positiveButton(R.string.delete) { callback() }
            negativeButton(android.R.string.cancel)
        }
    }

    internal fun showAddInstalledGamesDialog(
            context: Context,
            realm: Realm,
            gameNames: List<String>,
            callback: () -> Unit
    ) {
        MaterialDialog(context).show {
            title(text = "Selected games")
            listItemsMultiChoice(items = gameNames) { _, indices, items ->
                items.filterIndexed { index, _ -> isItemChecked(index) }
                        .forEach { realm.addGame(it) }
                if (indices.any { isItemChecked(it) }) callback()
            }
            positiveButton(R.string.add)
        }
    }

    internal fun showDeleteGamesDialog(context: Context, callback: () -> Unit) {
        MaterialDialog(context).show {
            title(R.string.dialog_delete_games)
            message(R.string.dialog_delete_games_msg)
            positiveButton(R.string.delete) { callback() }
            negativeButton(android.R.string.cancel)
        }
    }

    internal fun showImportSplitsDialog(context: Context, callback: (String) -> Unit) {
        MaterialDialog(context).show {
            title(R.string.dialog_import_splits)
            input(hintRes = R.string.hint_run_id)
            positiveButton(R.string.do_import) { dialog ->
                dialog.getInputField()?.let {
                    callback(it.text.toString())
                }
            }
        }
    }

    internal fun showImportSplitsOverwriteDialog(context: Context, callback: () -> Unit) {
        MaterialDialog(context).show {
            title(R.string.dialog_import_splits)
            message(R.string.dialog_import_splits_overwrite)
            positiveButton(android.R.string.ok) { callback() }
            negativeButton(android.R.string.cancel)
        }
    }

    internal fun showClearSplitsDialog(context: Context, callback: () -> Unit) {
        MaterialDialog(context).show {
            title(text = "Clear splits")
            message(R.string.dialog_clear_splits)
            positiveButton(android.R.string.ok) { callback() }
            negativeButton(android.R.string.cancel)
        }
    }

    internal fun showRemoveSplitsDialog(context: Context, callback: () -> Unit) {
        MaterialDialog(context).show {
            title(text = "Remove splits")
            message(text = "Are you sure?")
            positiveButton(android.R.string.ok) { callback() }
            negativeButton(android.R.string.cancel)
        }
    }
}
