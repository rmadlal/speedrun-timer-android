package il.ronmad.speedruntimer

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_splits.*

class SplitsFragment : BaseFragment() {

    lateinit var game: Game
    lateinit var category: Category
    lateinit var mListAdapter: SplitRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            val gameName = it.getString(ARG_GAME_NAME)
            val categoryName = it.getString(ARG_CATEGORY_NAME)
            game = realm.where<Game>().equalTo("name", gameName).findFirst()!!
            category = game.categories.where().equalTo("name", categoryName).findFirst()!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_splits, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActionBar?.title = game.name
        mActionBar?.subtitle = category.name
        mActionBar?.setDisplayHomeAsUpEnabled(true)
        setupRecyclerView()
        sobValueText.text = category.splits.fold(0L, { acc, split ->
            acc + split.bestTime
        }).getFormattedTime()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mActionBar?.subtitle = null
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            return when (it.itemId) {
                android.R.id.home -> {
                    activity.onBackPressed()
                    true
                }
                else -> false
            }
        }
        return false
    }

    override fun onFabAddPressed() {
        TODO("Add split dialog")
    }

    private fun setupRecyclerView() {
        mListAdapter = SplitRecyclerViewAdapter(category.splits)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mListAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
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
