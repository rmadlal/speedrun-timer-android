package il.ronmad.speedruntimer

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.widget.AdapterView
import kotlinx.android.synthetic.main.fragment_splits.*

class SplitsFragment : BaseFragment(R.layout.fragment_splits) {

    lateinit var category: Category
    var mAdapter: SplitAdapter? = null
    private var mActionMode: ActionMode? = null
    private var mActionModeCallback: MyActionModeCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val gameName = it.getString(ARG_GAME_NAME)
            val categoryName = it.getString(ARG_CATEGORY_NAME)
            category = realm.getCategoryByName(gameName, categoryName)!!
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActionBar?.title = category.getGame().name
        mActionBar?.subtitle = category.name
        mActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        setupActionMode()
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
            addSplit(name, position)
        }.show()
    }

    private fun addSplit(name: String, position: Int) {
        category.addSplit(name, position)
        mAdapter?.onItemAdded(position)
        mActionMode?.finish()
    }

    private fun editSplit(split: Split,
                          newName: String,
                          newPBTime: Long,
                          newBestTime: Long,
                          newPosition: Int) {
        split.updateData(newName, newPBTime, newBestTime)
        val position = split.getPosition()
        mAdapter?.onItemEdited(position)
        if (newPosition != position) {
            split.moveToPosition(newPosition)
            mAdapter?.onItemMoved(position, newPosition)
        }
        category.setPBFromSplits()
        calculateSob()
        mActionMode?.finish()
    }

    private fun removeSplits(toRemove: Collection<Long>) {
        if (toRemove.isEmpty()) return
        category.removeSplits(toRemove)
        mAdapter?.onItemsRemoved()
        category.setPBFromSplits()
        calculateSob()
        mActionMode?.finish()
    }

    private fun clearSplits() {
        category.clearSplits()
        mAdapter?.onItemsEdited()
        category.setPBFromSplits()
        calculateSob()
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
                    clearSplits()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun setupActionMode() {
        mActionModeCallback = MyActionModeCallback(mAdapter!!)
        mActionModeCallback?.apply {
            onEditPressed = {
                mAdapter?.selectedItems?.singleOrNull()?.let { id ->
                    category.getSplitById(id)?.let {
                        Dialogs.editSplitDialog(activity, it) { name, newPBTime, newBestTime, newPosition ->
                            editSplit(it, name, newPBTime, newBestTime, newPosition)
                        }.show()
                    }
                }
            }
            onDeletePressed = {
                mAdapter?.let {
                    if (it.selectedItems.isNotEmpty()) {
                        AlertDialog.Builder(activity)
                                .setTitle("Remove splits")
                                .setMessage("Are you sure?")
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    removeSplits(it.selectedItems)
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                    }
                }

            }
            onDestroy = { mActionMode = null }
        }
    }

    private fun setupRecyclerView() {
        mAdapter = SplitAdapter(category.splits, activity.getComparison())
        mAdapter?.apply {
            onItemClickListener = { _, position ->
                mActionMode?.let {
                    mAdapter?.toggleItemSelected(position)
                    it.invalidate()
                }
            }
            onItemLongClickListener = { _, position ->
                if (mActionMode == null) {
                    mAdapter?.toggleItemSelected(position)
                    mActionMode = activity.startActionMode(mActionModeCallback)
                    true
                } else false

            }
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            isNestedScrollingEnabled = false
        }
    }

    private fun setupComparisonSpinner() {
        comarisonSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                mAdapter?.comparison = when (position) {
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

