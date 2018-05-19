package il.ronmad.speedruntimer

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import android.widget.AdapterView
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_splits.*

class SplitsFragment : BaseFragment() {

    lateinit var category: Category
    lateinit var mRecyclerViewAdapter: SplitRecyclerViewAdapter
    private val realmChangeListener = RealmChangeListener<Realm> {
        mRecyclerViewAdapter.notifyDataSetChanged()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm.addChangeListener(realmChangeListener)

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mActionBar?.subtitle = null
    }

    override fun onDestroy() {
        realm.removeChangeListener(realmChangeListener)
        super.onDestroy()
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
        }.show()
    }

    private fun calculateSob() {
        sobValueText.text = category.splits.map { it.bestTime }.sum()
                .getFormattedTime(dashIfZero = true)
    }

    private fun onClearSplitsPressed() {
        AlertDialog.Builder(activity)
                .setTitle("Clear splits")
                .setMessage("Are you sure?")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    category.clearSplits()
                    category.setPBFromSplits()
                    calculateSob()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun setupRecyclerView() {
        mRecyclerViewAdapter = SplitRecyclerViewAdapter(category.splits, activity.getComparison())
        recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mRecyclerViewAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        }
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.START or ItemTouchHelper.END) {

            override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean {
                val sourceHolder = viewHolder as? SplitRecyclerViewAdapter.SplitViewHolder ?: return false
                val targetHolder = target as? SplitRecyclerViewAdapter.SplitViewHolder ?: return false
                mRecyclerViewAdapter.onItemMoved(sourceHolder.adapterPosition, targetHolder.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                (viewHolder as? SplitRecyclerViewAdapter.SplitViewHolder)?.let {
                    mRecyclerViewAdapter.onItemSwiped(it.adapterPosition)
                    category.setPBFromSplits()
                    calculateSob()
                }
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun setupComparisonSpinner() {
        comarisonSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                mRecyclerViewAdapter.onComparisonChanged(when (position) {
                    // Current Comparison
                    0 ->  activity.getComparison()
                    // Personal Best
                    1 -> Comparison.PERSONAL_BEST
                    // Best Segments
                    2 -> Comparison.BEST_SEGMENTS
                    else -> Comparison.PERSONAL_BEST
                })
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
