package il.ronmad.speedruntimer.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.widget.AdapterView
import il.ronmad.speedruntimer.*
import il.ronmad.speedruntimer.adapters.SplitAdapter
import il.ronmad.speedruntimer.realm.*
import il.ronmad.speedruntimer.web.SplitsIO
import kotlinx.android.synthetic.main.fragment_splits.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class SplitsFragment : BaseFragment(R.layout.fragment_splits) {

    lateinit var category: Category
    var mAdapter: SplitAdapter? = null
    private var mActionMode: ActionMode? = null
    private var mActionModeCallback: MyActionModeCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val gameName = it.getString(ARG_GAME_NAME)!!
            val categoryName = it.getString(ARG_CATEGORY_NAME)!!
            category = realm.getCategoryByName(gameName, categoryName)!!
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActionBar?.apply {
            title = category.gameName
            subtitle = category.name
            setDisplayHomeAsUpEnabled(true)
        }
        setupRecyclerView()
        setupActionMode()
        setupComparisonSpinner()
        updateSob()

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
                R.id.menu_import_splitsio -> {
                    onImportSplitsioPressed()
                    true
                }
                R.id.menu_export_splitsio -> {
                    onExportSplitsioPressed()
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

    private fun refresh() {
        mAdapter?.notifyDataSetChanged()
        updateSob()
        mActionMode?.finish()
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
        updateSob()
        mActionMode?.finish()
    }

    private fun removeSplits(toRemove: Collection<Long>) {
        if (toRemove.isEmpty()) return
        category.removeSplits(toRemove)
        category.setPBFromSplits()
        refresh()
    }

    private fun clearSplits() {
        category.clearSplits()
        category.setPBFromSplits()
        refresh()
    }

    private fun updateSob() {
        sobValueText?.text = category.calculateSob().getFormattedTime(dashIfZero = true)
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

    private fun onImportSplitsioPressed() {
        val importSplitsDialog = Dialogs.importSplitsDialog(activity) { id ->
            launch(UI) {
                splitsProgressBar?.visibility = View.VISIBLE
                val gameName = category.gameName
                val categoryName = category.name
                SplitsIO().getRun(id)?.let {
                    it.toRealmCategory(gameName, categoryName)
                    refresh()
                    context?.showToast("Splits imported successfully")
                } ?: context?.showToast("Import failed")
                splitsProgressBar?.visibility = View.GONE
            }
        }
        if (category.splits.isNotEmpty()) {
            AlertDialog.Builder(activity)
                    .setTitle("Import Splits from splits.io")
                    .setMessage("All existing data will be overwritten and lost. Import anyway?")
                    .setPositiveButton(android.R.string.ok) { _, _ -> importSplitsDialog.show() }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            return
        }
        importSplitsDialog.show()
    }

    private fun onExportSplitsioPressed() {
        if (category.splits.isEmpty()) {
            context?.showToast("There must be at least one split.")
            return
        }
        launch(UI) {
            context?.showToast("Uploading...")
            SplitsIO().uploadRun(category.toRun())?.let { claimUri ->
                context?.let {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(claimUri)))
                }
            } ?: context?.showToast("Upload failed")
        }
    }

    private fun setupActionMode() {
        mActionModeCallback = MyActionModeCallback(mAdapter!!).apply {
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
        mAdapter = SplitAdapter(category.splits, activity.getComparison()).apply {
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
            ViewCompat.setNestedScrollingEnabled(this, false)
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
        fun newInstance(gameName: String, categoryName: String) = SplitsFragment().apply {
            arguments = Bundle().also {
                it.putString(ARG_GAME_NAME, gameName)
                it.putString(ARG_CATEGORY_NAME, categoryName)
            }
        }
    }
}

