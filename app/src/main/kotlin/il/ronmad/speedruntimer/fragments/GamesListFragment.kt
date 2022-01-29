package il.ronmad.speedruntimer.fragments

import android.os.Bundle
import android.view.ActionMode
import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import il.ronmad.speedruntimer.Dialogs
import il.ronmad.speedruntimer.R
import il.ronmad.speedruntimer.TAG_GAME_FRAGMENT
import il.ronmad.speedruntimer.adapters.GameAdapter
import il.ronmad.speedruntimer.databinding.FragmentGamesListBinding
import il.ronmad.speedruntimer.realm.*
import io.realm.kotlin.where

class GamesListFragment : BaseFragment<FragmentGamesListBinding>(FragmentGamesListBinding::inflate) {

    private var mAdapter: GameAdapter? = null
    private var mActionMode: ActionMode? = null
    private var mActionModeCallback: MyActionModeCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActionBar?.apply {
            title = activity.getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupActionMode()
        checkEmptyList()

        fabAdd.setOnClickListener { onFabAddPressed() }
        fabAdd.show()
    }

    override fun onFabAddPressed() {
        Dialogs.showNewGameDialog(activity, realm) {
            addGame(it)
        }
    }

    fun refreshList() {
        mAdapter?.notifyDataSetChanged()
        checkEmptyList()
        mActionMode?.finish()
    }

    private fun checkEmptyList() {
        viewBinding.emptyList.visibility = if (realm.where<Game>().count() == 0L) View.VISIBLE else View.GONE
    }

    private fun addGame(name: String) {
        realm.addGame(name)
        mAdapter?.onItemAdded()
        checkEmptyList()
        mActionMode?.finish()
    }

    private fun editGameName(game: Game, newName: String) {
        game.setGameName(newName)
        mAdapter?.onItemsEdited()
        mActionMode?.finish()
    }

    private fun removeGames(toRemove: Collection<Long>) {
        if (toRemove.isEmpty()) return
        realm.removeGames(toRemove)
        mAdapter?.onItemsRemoved()
        checkEmptyList()
        mActionMode?.finish()
    }

    private fun setupActionMode() {
        mActionModeCallback = MyActionModeCallback(mAdapter!!).apply {
            onEditPressed = {
                mAdapter?.selectedItems?.singleOrNull()?.let { id ->
                    realm.getGameById(id)?.let { game ->
                        Dialogs.showEditGameDialog(activity, game) {
                            editGameName(game, it)
                        }
                    }
                }
            }
            onDeletePressed = {
                mAdapter?.let {
                    if (it.selectedItems.isNotEmpty()) {
                        Dialogs.showDeleteGamesDialog(activity) {
                            removeGames(it.selectedItems)
                        }
                    }
                }
            }
            onDestroy = { mActionMode = null }
        }
    }

    private fun setupRecyclerView() {
        mAdapter = GameAdapter(realm.where<Game>().findAll()).apply {
            onItemClickListener = { holder, position ->
                if (mActionMode == null) {
                    val game = holder.item
                    activity.supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.fade_in, R.anim.fade_out,
                            R.anim.fade_in, R.anim.fade_out
                        )
                        .replace(
                            R.id.fragment_container,
                            GameFragment.newInstance(game.name),
                            TAG_GAME_FRAGMENT
                        )
                        .addToBackStack(null)
                        .commitAllowingStateLoss()
                } else {
                    mAdapter?.toggleItemSelected(position)
                    mActionMode?.invalidate()
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
        viewBinding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            ViewCompat.setNestedScrollingEnabled(this, false)
        }
    }
}
