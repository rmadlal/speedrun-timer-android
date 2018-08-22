package il.ronmad.speedruntimer

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem

class MyActionModeCallback(private val adapter: BaseRecyclerViewAdapter<*>) : ActionMode.Callback {

    var onEditPressed: (() -> Unit)? = null
    var onDeletePressed: (() -> Unit)? = null
    var onDestroy: (() -> Unit)? = null

    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        actionMode.menuInflater.inflate(R.menu.list_context_menu, menu)
        return true
    }

    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        if (adapter.selectedItems.isEmpty()) {
            actionMode.finish()
        } else {
            actionMode.title = adapter.selectedItems.size.toString()
            menu.findItem(R.id.menu_edit).isVisible = adapter.selectedItems.size == 1
        }
        return true
    }

    override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_edit -> {
                onEditPressed?.invoke()
                true
            }
            R.id.menu_delete -> {
                onDeletePressed?.invoke()
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(actionMode: ActionMode) {
        adapter.clearSelections()
        onDestroy?.invoke()
    }
}