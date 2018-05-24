package il.ronmad.speedruntimer

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.widget.AdapterView
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_splits.*

class SplitsFragment : BaseFragment(), TimeExtensions {

    lateinit var category: Category
    lateinit var mRecyclerViewAdapter: SplitRecyclerViewAdapter
    private var mActionMode: ActionMode? = null
    private val mActionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            actionMode.menuInflater.inflate(R.menu.splits_fragment_context_menu, menu)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
            actionMode.title = mRecyclerViewAdapter.selectedItems.size.toString()
            menu.findItem(R.id.menu_edit).isVisible = mRecyclerViewAdapter.selectedItems.size == 1
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
            mRecyclerViewAdapter.clearSelections()
            mActionMode = null
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val gameName = it.getString(ARG_GAME_NAME)
            val categoryName = it.getString(ARG_CATEGORY_NAME)
            category = realm.where<Category>()
                    .equalTo("game.name", gameName)
                    .equalTo("name", categoryName).findFirst()!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_splits, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActionBar?.title = category.getGame().name
        mActionBar?.subtitle = category.name
        mActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        setupComparisonSpinner()
        calculateSob()

        fabAdd.setOnClickListener { onFabAddPressed() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mActionBar?.subtitle = null
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.splits_actions, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            return when (it.itemId) {
                android.R.id.home -> {
                    activity.onBackPressed()
                    true
                }
                R.id.menu_clear_splits -> {
                    onClearSplitsPressed()
                    true
                }
                else -> false
            }
        }
        return false
    }

    override fun onFabAddPressed() {
        Dialogs.newSplitDialog(activity, category) { name, position ->
            category.addSplit(name, position)
            mRecyclerViewAdapter.onItemAdded(position)
            mActionMode?.finish()
        }.show()
    }

    fun onMenuEditPressed() {
        category.splits.where()
                .equalTo("id", mRecyclerViewAdapter.selectedItems.first())
                .findFirst()?.let {
                    Dialogs.editSplitDialog(activity, it) { name, newPBTime, newBestTime, newPosition ->
                        it.updateData(name, newPBTime, newBestTime)
                        mRecyclerViewAdapter.onItemEdited(category.splits.indexOf(it))
                        if (newPosition != it.getPosition()) {
                            it.moveToPosition(newPosition)
                            mRecyclerViewAdapter.onItemMoved(it.getPosition(), newPosition)
                        }
                        category.setPBFromSplits()
                        calculateSob()
                        mActionMode?.finish()
                    }.show()
                }
    }

    fun onMenuDeletePressed() {
        AlertDialog.Builder(activity)
                .setTitle("Remove splits")
                .setMessage("Are you sure?")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    category.removeSplits(mRecyclerViewAdapter.selectedItems)
                    mRecyclerViewAdapter.onItemsRemoved()
                    category.setPBFromSplits()
                    calculateSob()
                    mActionMode?.finish()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun calculateSob() {
        sobValueText.text = category.splits.map { it.bestTime }.sum()
                .getFormattedTime(dashIfZero = true)
    }

    private fun onClearSplitsPressed() {
        AlertDialog.Builder(activity)
                .setTitle("Clear splits")
                .setMessage("PB and Best Segments will be lost. Are you sure?")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    category.clearSplits()
                    mRecyclerViewAdapter.onItemsEdited()
                    category.setPBFromSplits()
                    calculateSob()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun setupRecyclerView() {
        mRecyclerViewAdapter = SplitRecyclerViewAdapter(category.splits, activity.getComparison(),
                { holder, position ->   // onItemClickListener
                    mActionMode ?: return@SplitRecyclerViewAdapter
                    mRecyclerViewAdapter.toggleItemSelected(position)
                    if (mRecyclerViewAdapter.selectedItems.isEmpty()) {
                        mActionMode?.finish()
                    } else {
                        mActionMode?.invalidate()
                    }
                }) { holder, position ->    // onItemLongClickListener
            if (mActionMode != null) return@SplitRecyclerViewAdapter false
            mRecyclerViewAdapter.toggleItemSelected(position)
            mActionMode = activity.startActionMode(mActionModeCallback)
            true
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mRecyclerViewAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            isNestedScrollingEnabled = false
        }
    }

    private fun setupComparisonSpinner() {
        comarisonSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                mRecyclerViewAdapter.comparison = when (position) {
                // Current Comparison
                    0 ->  activity.getComparison()
                // Personal Best
                    1 -> Comparison.PERSONAL_BEST
                // Best Segments
                    2 -> Comparison.BEST_SEGMENTS
                    else -> Comparison.PERSONAL_BEST
                }
            }
        }
    }

    companion object {
        fun newInstance(gameName: String, categoryName: String): SplitsFragment {
            val fragment = SplitsFragment()
            val args = Bundle()
            args.putString(ARG_GAME_NAME, gameName)
            args.putString(ARG_CATEGORY_NAME, categoryName)
            fragment.arguments = args
            return fragment
        }
    }
}

