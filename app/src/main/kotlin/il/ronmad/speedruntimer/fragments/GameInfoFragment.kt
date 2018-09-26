package il.ronmad.speedruntimer.fragments

import android.os.Bundle
import android.support.v4.view.ViewCompat
import il.ronmad.speedruntimer.ARG_GAME_NAME
import il.ronmad.speedruntimer.R
import il.ronmad.speedruntimer.adapters.InfoListAdapter
import il.ronmad.speedruntimer.app
import il.ronmad.speedruntimer.realm.Game
import il.ronmad.speedruntimer.realm.getGameByName
import il.ronmad.speedruntimer.showToast
import il.ronmad.speedruntimer.web.SrcLeaderboard
import io.realm.Realm
import io.realm.RealmChangeListener
import kotlinx.android.synthetic.main.fragment_game.*
import kotlinx.android.synthetic.main.fragment_game_info.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class GameInfoFragment : BaseFragment(R.layout.fragment_game_info) {

    private val realmChangeListener = RealmChangeListener<Realm> { mAdapter?.notifyDataSetChanged() }
    private lateinit var game: Game
    private var mAdapter: InfoListAdapter? = null
    internal var isDataShowing = false
    private var refreshJob: Job? = null

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
        setupListView()
        swipeRefreshLayout.setOnRefreshListener { refreshData() }
    }

    override fun onDestroy() {
        realm.removeChangeListener(realmChangeListener)
        super.onDestroy()
    }

    override fun onFabAddPressed() {}

    internal fun refreshData() {
        refreshJob = launch(UI) {
            val app = context?.app ?: run {
                displayData(emptyList())
                refreshJob = null
                return@launch
            }
            swipeRefreshLayout?.isRefreshing = true
            val leaderboards = app.srcApi.fetchLeaderboardsForGame(context, game.name)
            if (isActive) {
                displayData(leaderboards)
            }
            swipeRefreshLayout?.isRefreshing = false
            refreshJob = null
        }
    }

    private fun setupListView() {
        mAdapter = InfoListAdapter(context, game)
        expandableListView.setAdapter(mAdapter)
        ViewCompat.setNestedScrollingEnabled(expandableListView, true)
    }

    private fun displayData(data: List<SrcLeaderboard>) {
        mAdapter?.clear()
        if (data.isEmpty()) {
            context?.showToast("No data available")
            (parentFragment as? GameFragment)?.viewPager?.currentItem = 0
        } else {
            mAdapter?.data = data
        }
        isDataShowing = !data.isEmpty()
    }

    companion object {

        fun newInstance(gameName: String) = GameInfoFragment().apply {
            arguments = Bundle().also { it.putString(ARG_GAME_NAME, gameName) }
        }
    }
}
