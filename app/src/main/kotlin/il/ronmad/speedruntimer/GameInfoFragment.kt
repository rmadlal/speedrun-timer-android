package il.ronmad.speedruntimer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_game.*
import kotlinx.android.synthetic.main.fragment_game_info.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class GameInfoFragment : BaseFragment() {

    private val realmChangeListener = RealmChangeListener<Realm> { adapter.notifyDataSetChanged() }
    private lateinit var game: Game
    private var pbs: Map<String, Long> = mapOf()
    private lateinit var adapter: InfoListAdapter
    internal var isDataShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm.addChangeListener(realmChangeListener)
        arguments?.let {
            val gameName = it.getString(ARG_GAME_NAME)
            game = realm.where<Game>().equalTo("name", gameName).findFirst()!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_game_info, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupListView()
        swipeRefreshLayout.setOnRefreshListener { refreshData(true) }
    }

    override fun onDestroy() {
        realm.removeChangeListener(realmChangeListener)
        super.onDestroy()
    }

    override fun onFabAddPressed() {}

    internal fun refreshData(forceFetch: Boolean = false) {
        pbs = mapOf()
        game.categories.forEach { pbs += it.name.toLowerCase() to it.bestTime }
        launch(UI) {
            swipeRefreshLayout.isRefreshing = true
            val application = getContext()?.applicationContext as? MyApplication
            val leaderboards = if (!forceFetch) {
                application?.srcLeaderboardCache?.getOrElse(game.name) {
                    Src.fetchLeaderboardsForGame(getContext(), game.name)
                } ?: Src.fetchLeaderboardsForGame(getContext(), game.name)
            }
            else Src.fetchLeaderboardsForGame(getContext(), game.name)
            displayData(leaderboards)
            if (getContext() == null) return@launch
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupListView() {
        adapter = InfoListAdapter(context, game)
        listView.adapter = adapter
    }

    private fun displayData(data: List<SrcLeaderboard>) {
        if (context == null) return
        adapter.clear()
        if (data.isEmpty()) {
            Toast.makeText(context, "No data available", Toast.LENGTH_SHORT).show()
            (parentFragment as? GameFragment)?.viewPager?.currentItem = 0
        } else {
            adapter.data = data
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
