package il.ronmad.speedruntimer

import android.os.Bundle
import io.realm.Realm
import io.realm.RealmChangeListener
import kotlinx.android.synthetic.main.fragment_game.*
import kotlinx.android.synthetic.main.fragment_game_info.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import android.support.v4.view.ViewCompat
import kotlinx.coroutines.experimental.Job

class GameInfoFragment : BaseFragment(R.layout.fragment_game_info) {

    private val realmChangeListener = RealmChangeListener<Realm> { mAdapter?.notifyDataSetChanged() }
    private lateinit var game: Game
    private var pbs: Map<String, Long> = mapOf()
    private var mAdapter: InfoListAdapter? = null
    internal var isDataShowing = false
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm.addChangeListener(realmChangeListener)
        arguments?.let {
            val gameName = it.getString(ARG_GAME_NAME)
            game = realm.getGameByName(gameName)!!
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupListView()
        swipeRefreshLayout.setOnRefreshListener { refreshData() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        refreshJob?.cancel()
    }

    override fun onDestroy() {
        realm.removeChangeListener(realmChangeListener)
        super.onDestroy()
    }

    override fun onFabAddPressed() {}

    internal fun refreshData() {
        pbs = game.categories.map { it.name.toLowerCase() to it.bestTime }.toMap()
        refreshJob = launch(UI) {
            swipeRefreshLayout.isRefreshing = true
            val leaderboards = Src.fetchLeaderboardsForGame(getContext(), game.name)
            if (isActive) {
                displayData(leaderboards)
                swipeRefreshLayout.isRefreshing = false
            }
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

        fun newInstance(gameName: String): GameInfoFragment {
            val fragment = GameInfoFragment()
            val args = Bundle()
            args.putString(ARG_GAME_NAME, gameName)
            fragment.arguments = args
            return fragment
        }
    }
}
