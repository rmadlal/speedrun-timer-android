package il.ronmad.speedruntimer

import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager
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
import kotlinx.android.synthetic.main.edit_time_layout.view.*
import kotlinx.android.synthetic.main.new_category_dialog.view.*
import kotlinx.android.synthetic.main.new_split_dialog.view.*

object Dialogs {

    internal fun showNewGameDialog(context: Context, realm: Realm, callback: (String) -> Unit) {
        MaterialDialog(context).show {
            title(text = "New game")
            input(hintRes = R.string.hint_game_title)
            positiveButton(R.string.create) {
                getInputField().run {
                    if (isValidForGame(realm)) {
                        callback(text.toString())
                        dismiss()
                    }
                }
            }
            negativeButton(android.R.string.cancel) { dismiss() }
            noAutoDismiss()
        }
    }

    internal fun showNewCategoryDialog(context: Context, game: Game, callback: (String) -> Unit) {
        val dialogView = View.inflate(context, R.layout.new_category_dialog, null)
        MaterialDialog(context).show {
            title(text = "New category")
            customView(view = dialogView).apply {
                getCustomView().run {
                    newCategoryInput.setCategories(game.name)
                    newCategoryInput.onTextChanged {
                        setActionButtonEnabled(WhichButton.POSITIVE, !it.isNullOrBlank())
                    }
                }
            }
            positiveButton(R.string.create) {
                getCustomView().run {
                    if (newCategoryInput.isValidForCategory(game)) {
                        callback(newCategoryInput.text.toString())
                        dismiss()
                    }
                }
            }
            negativeButton(android.R.string.cancel) { dismiss() }
            setActionButtonEnabled(WhichButton.POSITIVE, false)
            noAutoDismiss()
        }
    }

    fun showNewSplitDialog(context: Context, category: Category, callback: (String, Int) -> Unit) {
        MaterialDialog(context).show {
            title(text = "New split")
            customView(R.layout.new_split_dialog).apply {
                getCustomView().run {
                    newSplitInput.onTextChanged {
                        setActionButtonEnabled(WhichButton.POSITIVE, !it.isNullOrBlank())
                    }
                    positionSpinner.apply {
                        adapter = SplitPositionSpinnerAdapter(context,
                                category.splits.size + 1)
                        setSelection(count - 1)
                    }
                }
            }
            positiveButton(R.string.create) {
                getCustomView().run {
                    if (newSplitInput.isValidForSplit(category)) {
                        val position = positionSpinner.selectedItem as Int - 1
                        callback(newSplitInput.text.toString(), position)
                        dismiss()
                    }
                }
            }
            negativeButton(android.R.string.cancel) { dismiss() }
            setActionButtonEnabled(WhichButton.POSITIVE, false)
            noAutoDismiss()
        }
    }

    internal fun showEditGameDialog(context: Context, game: Game, callback: (String) -> Unit) {
        MaterialDialog(context).show {
            title(text = "Edit name")
            input(hintRes = R.string.hint_game_title, prefill = game.name)
            positiveButton(R.string.save) {
                getInputField().run {
                    if (text.toString() == game.name) {
                        dismiss()
                        return@run
                    }
                    if (isValidForGame(game.realm)) {
                        callback(text.toString())
                        dismiss()
                    }
                }
            }
            negativeButton(android.R.string.cancel) { dismiss() }
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
            customView(R.layout.edit_category_dialog).apply {
                getCustomView().run {
                    categoryName.apply {
                        setText(category.name)
                        setSelection(text.length)
                        onTextChanged {
                            setActionButtonEnabled(WhichButton.POSITIVE, !it.isNullOrBlank())
                        }
                    }
                    if (category.bestTime > 0) {
                        editTime.setEditTextsFromTime(category.bestTime)
                    }
                    runCount.setText(category.runCount.toString())
                    editTime.clearTimeButton.setOnClickListener {
                        editTime.setEditTextsFromTime(0L)
                    }
                }
            }
            positiveButton(R.string.save) {
                getCustomView().run {
                    val newName = categoryName.text.toString()
                    if (newName == category.name
                            || categoryName.isValidForCategory(category.getGame())) {
                        val newTime = editTime.getTimeFromEditTexts()
                        val newRunCountStr = runCount.text.toString()
                        val newRunCount = if (newRunCountStr.isEmpty()) 0 else Integer.parseInt(newRunCountStr)
                        callback(newName, newTime, newRunCount)
                        dismiss()
                    }
                }
            }
            negativeButton(android.R.string.cancel) { dismiss() }
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
            customView(R.layout.edit_split_dialog).apply {
                getCustomView().run {
                    editPositionSpinner.apply {
                        adapter = SplitPositionSpinnerAdapter(context,
                                split.getCategory().splits.size)
                        setSelection(split.getPosition())
                    }
                    nameInput.apply {
                        setText(split.name)
                        setSelection(text.length)
                        onTextChanged {
                            setActionButtonEnabled(WhichButton.POSITIVE, !it.isNullOrBlank())
                        }
                    }
                    if (split.pbTime > 0) {
                        editTimePB.setEditTextsFromTime(split.pbTime)
                    }
                    if (split.bestTime > 0) {
                        editTimeBest.setEditTextsFromTime(split.bestTime)
                    }
                    editTimePB.clearTimeButton.setOnClickListener {
                        editTimePB.setEditTextsFromTime(0L)
                    }
                    editTimeBest.clearTimeButton.setOnClickListener {
                        editTimeBest.setEditTextsFromTime(0L)
                    }
                }
            }
            positiveButton(R.string.save) {
                getCustomView().run {
                    val newName = nameInput.text.toString()
                    if (newName == split.name
                            || nameInput.isValidForSplit(split.getCategory())) {
                        val newPBSegmentTime = editTimePB.getTimeFromEditTexts()
                        val newBestSegmentTime = editTimeBest.getTimeFromEditTexts()
                        val position = editPositionSpinner.selectedItem as Int - 1
                        callback(newName, newPBSegmentTime, newBestSegmentTime, position)
                        dismiss()
                    }
                }

            }
            negativeButton(android.R.string.cancel) { dismiss() }
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
                        .forEach { realm.addGame(it.toString()) }
                if (indices.any { isItemChecked(it) }) callback()
            }
            positiveButton(R.string.add)
            negativeButton(android.R.string.cancel)
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

    internal fun showImportSplitsDialog(
            context: Context,
            overwrite: Boolean,
            callback: (String) -> Unit
    ) {
        fun MaterialDialog.doImportMode() {
            message(R.string.dialog_import_splits_msg)
            input(hintRes = R.string.hint_run_id)
            positiveButton(R.string.do_import) {
                getInputField().run { callback(text.toString()) }
                dismiss()
            }
        }
        MaterialDialog(context).show {
            title(R.string.dialog_import_splits)
            if (overwrite) {
                message(R.string.dialog_import_splits_overwrite)
                positiveButton(android.R.string.ok) {
                    clearPositiveListeners()
                    doImportMode()
                }
            } else {
                doImportMode()
            }
            negativeButton(android.R.string.cancel) { dismiss() }
            noAutoDismiss()
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
