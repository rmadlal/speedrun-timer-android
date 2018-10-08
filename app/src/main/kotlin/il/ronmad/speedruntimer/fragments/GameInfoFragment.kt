package il.ronmad.speedruntimer.fragments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.view.ViewCompat
import il.ronmad.speedruntimer.ARG_GAME_NAME
import il.ronmad.speedruntimer.R
import il.ronmad.speedruntimer.adapters.InfoListAdapter
import il.ronmad.speedruntimer.getExpandedGroupPositions
import il.ronmad.speedruntimer.realm.Game
import il.ronmad.speedruntimer.realm.getGameByName
import il.ronmad.speedruntimer.showToast
import il.ronmad.speedruntimer.ui.GameInfoViewModel
import il.ronmad.speedruntimer.web.SrcLeaderboard
import io.realm.Realm
import io.realm.RealmChangeListener
import kotlinx.android.synthetic.main.fragment_game.*
import kotlinx.android.synthetic.main.fragment_game_info.*

class GameInfoFragment : BaseFragment(R.layout.fragment_game_info) {

    private val realmChangeListener = RealmChangeListener<Realm> { mAdapter?.notifyDataSetChanged() }
    private lateinit var game: Game
    private var mAdapter: InfoListAdapter? = null
    internal var isDataShowing = false

    private lateinit var viewModel: GameInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm.addChangeListener(realmChangeListener)
        arguments?.let {
            val gameName = it.getString(ARG_GAME_NAME)!!
            game = realm.getGameByName(gameName)!!
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val expandedGroups = savedInstanceState?.let {
            it.getIntArray(KEY_LIST_EXPANDED_GROUPS)?.toList()
        }.orEmpty()

        setupListView(expandedGroups)

        viewModel = ViewModelProviders.of(this).get(GameInfoViewModel::class.java).apply {
            refreshSpinner.observe(this@GameInfoFragment, Observer { refreshing ->
                refreshing?.let {
                    swipeRefreshLayout?.isRefreshing = it
                }
            })
            leaderboards.observe(this@GameInfoFragment, Observer { leaderboards ->
                leaderboards?.let {
                    displayData(it)
                }
            })
        }

        swipeRefreshLayout.setOnRefreshListener { refreshData() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putIntArray(KEY_LIST_EXPANDED_GROUPS,
                    expandableListView?.getExpandedGroupPositions()?.toIntArray() ?: IntArray(0))
        }
    }

    override fun onDestroy() {
        realm.removeChangeListener(realmChangeListener)
        super.onDestroy()
    }

    override fun onFabAddPressed() {}

    internal fun refreshData() {
        viewModel.refreshInfo(game.name)
    }

    private fun setupListView(expandedGroups: List<Int> = emptyList()) {
        mAdapter = InfoListAdapter(context, game, expandedGroups)
        expandableListView.apply {
            setAdapter(mAdapter)
            ViewCompat.setNestedScrollingEnabled(this, true)
        }
    }

    private fun displayData(data: List<SrcLeaderboard>) {
        mAdapter?.clear()
        if (data.isEmpty()) {
            context?.showToast("No data available")
            (parentFragment as? GameFragment)?.viewPager?.currentItem = 0
        } else {
            mAdapter?.data = data
        }
        isDataShowing = data.isNotEmpty()
    }

    companion object {

        fun newInstance(gameName: String) = GameInfoFragment().apply {
            arguments = Bundle().also { it.putString(ARG_GAME_NAME, gameName) }
        }

        const val KEY_LIST_EXPANDED_GROUPS = "ListExpandedGroups"
    }
}
