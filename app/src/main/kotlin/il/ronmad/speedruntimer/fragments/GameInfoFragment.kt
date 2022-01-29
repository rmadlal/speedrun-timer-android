package il.ronmad.speedruntimer.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import il.ronmad.speedruntimer.ARG_GAME_NAME
import il.ronmad.speedruntimer.adapters.InfoListAdapter
import il.ronmad.speedruntimer.databinding.FragmentGameInfoBinding
import il.ronmad.speedruntimer.getExpandedGroupPositions
import il.ronmad.speedruntimer.realm.Game
import il.ronmad.speedruntimer.realm.getGameByName
import il.ronmad.speedruntimer.showToast
import il.ronmad.speedruntimer.ui.GameInfoViewModel
import il.ronmad.speedruntimer.ui.ToastFetchEmpty
import il.ronmad.speedruntimer.ui.ToastFetchError
import io.realm.Realm
import io.realm.RealmChangeListener

class GameInfoFragment : BaseFragment<FragmentGameInfoBinding>(FragmentGameInfoBinding::inflate) {

    private val realmChangeListener = RealmChangeListener<Realm> { mAdapter?.notifyDataSetChanged() }
    private lateinit var game: Game
    private var mAdapter: InfoListAdapter? = null
    internal var isDataShowing = false

    private lateinit var viewModel: GameInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm.addChangeListener(realmChangeListener)
        val gameName = requireArguments().getString(ARG_GAME_NAME)!!
        game = realm.getGameByName(gameName)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val expandedGroups = savedInstanceState?.let {
            it.getIntArray(KEY_LIST_EXPANDED_GROUPS)?.toList()
        }.orEmpty()

        setupListView(expandedGroups)

        viewModel = ViewModelProvider(this).get(GameInfoViewModel::class.java).apply {
            refreshSpinner.observe(viewLifecycleOwner) { refreshing ->
                refreshing?.let {
                    viewBinding.swipeRefreshLayout.isRefreshing = it
                }
            }
            leaderboards.observe(viewLifecycleOwner) { leaderboards ->
                leaderboards?.let {
                    mAdapter?.data = it
                    isDataShowing = it.isNotEmpty()
                }
            }
            toast.observe(viewLifecycleOwner) { toast ->
                toast?.handle()?.let {
                    context?.showToast(it.message)
                    when (it) {
                        is ToastFetchEmpty, is ToastFetchError ->
                            (parentFragment as? GameFragment)?.viewBinding?.viewPager?.apply {
                                currentItem = GameFragment.TAB_CATEGORIES
                            }
                    }
                }
            }
        }

        viewBinding.swipeRefreshLayout.setOnRefreshListener { refreshData() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putIntArray(
                KEY_LIST_EXPANDED_GROUPS,
                viewBinding.expandableListView.getExpandedGroupPositions().toIntArray()
            )
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
        viewBinding.expandableListView.apply {
            setAdapter(mAdapter)
            ViewCompat.setNestedScrollingEnabled(this, true)
        }
    }

    companion object {

        fun newInstance(gameName: String) = GameInfoFragment().apply {
            arguments = Bundle().also { it.putString(ARG_GAME_NAME, gameName) }
        }

        const val KEY_LIST_EXPANDED_GROUPS = "ListExpandedGroups"
    }
}
