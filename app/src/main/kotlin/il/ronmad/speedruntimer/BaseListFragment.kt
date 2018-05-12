package il.ronmad.speedruntimer

import android.os.Bundle
import android.support.v4.app.ListFragment
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView

import io.realm.Realm
import io.realm.RealmChangeListener
import kotlinx.android.synthetic.main.activity_main.*

abstract class BaseListFragment<T> : ListFragment() {

    protected lateinit var realm: Realm
    private val realmChangeListener = RealmChangeListener<Realm> { update() }
    protected lateinit var mListAdapter: MyBaseListFragmentAdapter<T>
    protected var mActionMode: ActionMode? = null
    protected var layoutResId: Int = 0
    protected var contextMenuResId: Int = 0

    val selectedItems: List<T>
        get() = mListAdapter.checkedItems

    protected val activity: MainActivity
        get() = getActivity() as MainActivity

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            val inflater = actionMode.menuInflater
            inflater.inflate(contextMenuResId, menu)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            actionMode.title = selectedItems.size.toString()
            menu.findItem(R.id.menu_edit).isVisible = selectedItems.size == 1
            return true
        }

        override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_edit -> {
                    onMenuEditPressed()
                    true
                }
                R.id.menu_delete -> {
                    onMenuDeletePressed()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(actionMode: ActionMode) {
            mListAdapter.clearSelections()
            mActionMode = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()
        realm.addChangeListener(realmChangeListener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(layoutResId, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupListView()
        activity.fabAdd.setOnClickListener { onFabAddPressed() }
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    override fun onPause() {
        super.onPause()
        finishActionMode()
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.removeChangeListener(realmChangeListener)
        realm.close()
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        mActionMode?.let {
            mListAdapter.setItemChecked(position, !mListAdapter.isItemChecked(position))
            if (selectedItems.isEmpty()) {
                it.finish()
            } else {
                it.invalidate()
            }
        }
    }

    abstract fun update()

    abstract fun onFabAddPressed()

    abstract fun onMenuEditPressed()

    abstract fun onMenuDeletePressed()

    fun finishActionMode() {
        mActionMode?.finish()
    }

    private fun setupListView() {
        listView.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
        listView.setOnItemLongClickListener { _, _, i, _ ->
            if (mActionMode != null) {
                return@setOnItemLongClickListener false
            }
            mActionMode = activity.startActionMode(actionModeCallback)
            mListAdapter.setItemChecked(i, true)
            mActionMode?.invalidate()
            true
        }
    }
}
