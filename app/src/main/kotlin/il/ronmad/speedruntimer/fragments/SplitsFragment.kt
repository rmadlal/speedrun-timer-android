package il.ronmad.speedruntimer.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import il.ronmad.speedruntimer.*
import il.ronmad.speedruntimer.adapters.SplitAdapter
import il.ronmad.speedruntimer.realm.*
import il.ronmad.speedruntimer.ui.SplitsIOViewModel
import kotlinx.android.synthetic.main.fragment_splits.*

class SplitsFragment : BaseFragment(R.layout.fragment_splits) {

    lateinit var category: Category
    var mAdapter: SplitAdapter? = null
    private var mActionMode: ActionMode? = null
    private var mActionModeCallback: MyActionModeCallback? = null

    private lateinit var splitsIOViewModel: SplitsIOViewModel

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
        splitsIOViewModel = ViewModelProvider(this).get(SplitsIOViewModel::class.java)
        splitsIOViewModel.apply {
            importedRun.observe(viewLifecycleOwner, { run ->
                run?.handle()?.let {
                    it.toRealmCategory(category.gameName, category.name)
                    refresh()
                }
            })
            progressBar.observe(viewLifecycleOwner, { progress ->
                progress?.let {
                    splitsProgressBar?.visibility = if (it) View.VISIBLE else View.GONE
                }
            })
            claimUri.observe(viewLifecycleOwner, { uri ->
                uri?.handle()?.let {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                }
            })
            toast.observe(viewLifecycleOwner, { toast ->
                toast?.handle()?.let {
                    context?.showToast(it.message)
                }
            })
        }

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.splits_actions, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity.onBackPressed()
                true
            }
            R.id.menu_export_splitsio -> {
                onExportSplitsioPressed()
                true
            }
            R.id.menu_import_splitsio -> {
                onImportSplitsioPressed()
                true
            }
            R.id.menu_clear_splits -> {
                onClearSplitsPressed()
                true
            }
            else -> false
        }
    }

    override fun onFabAddPressed() {
        Dialogs.showNewSplitDialog(activity, category) { name, position ->
            addSplit(name, position)
        }
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

    private fun editSplit(
            split: Split,
            newName: String,
            newPBTime: Long,
            newBestTime: Long,
            newPosition: Int
    ) {
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
        Dialogs.showClearSplitsDialog(activity) {
            clearSplits()
        }
    }

    private fun onExportSplitsioPressed() {
        if (category.splits.isEmpty()) {
            context?.showToast("There must be at least one split.")
            return
        }
        context?.showToast("Uploading...")
        splitsIOViewModel.exportRun(category.toRun())
    }

    private fun onImportSplitsioPressed() {
        Dialogs.showImportSplitsDialog(activity, category.splits.isNotEmpty()) { id ->
            splitsIOViewModel.importRun(id)
        }
    }

    private fun setupActionMode() {
        mActionModeCallback = MyActionModeCallback(mAdapter!!).apply {
            onEditPressed = {
                mAdapter?.selectedItems?.singleOrNull()?.let { id ->
                    category.getSplitById(id)?.let {
                        Dialogs.showEditSplitDialog(activity, it) { name, newPBTime, newBestTime, newPosition ->
                            editSplit(it, name, newPBTime, newBestTime, newPosition)
                        }
                    }
                }
            }
            onDeletePressed = {
                mAdapter?.let {
                    if (it.selectedItems.isNotEmpty()) {
                        Dialogs.showRemoveSplitsDialog(activity) {
                            removeSplits(it.selectedItems)
                        }
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
                    0 -> activity.getComparison()
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

