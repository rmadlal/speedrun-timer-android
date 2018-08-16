package il.ronmad.speedruntimer

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.ActionMode
import android.view.View
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_games_list.*

class GamesListFragment : BaseFragment(R.layout.fragment_games_list) {

    private var mAdapter: GameAdapter? = null
    private var mActionMode: ActionMode? = null
    private var mActionModeCallback: MyActionModeCallback? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActionBar?.title = activity.getString(R.string.app_name)
        mActionBar?.setDisplayHomeAsUpEnabled(false)

        setupRecyclerView()
        setupActionMode()
        checkEmptyList()

        fabAdd.setOnClickListener { onFabAddPressed() }
        fabAdd.show()
    }

    override fun onFabAddPressed() {
        Dialogs.newGameDialog(activity, realm) {
            addGame(it)
        }.show()
    }

    fun refreshList() {
        mAdapter?.notifyDataSetChanged()
        checkEmptyList()
        mActionMode?.finish()
    }

    private fun checkEmptyList() {
        emptyList?.visibility = if (realm.where<Game>().count() == 0L) View.VISIBLE else View.GONE
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
        realm.removeGames(toRemove)
        mAdapter?.onItemsRemoved()
        checkEmptyList()
        mActionMode?.finish()
    }

    private fun setupActionMode() {
        mActionModeCallback = MyActionModeCallback(mAdapter!!)
        mActionModeCallback?.onEditPressed = {
            mAdapter?.let { adapter ->
                realm.getGameById(adapter.selectedItems.first())?.let { game ->
                    Dialogs.editGameDialog(activity, realm, game) {
                        editGameName(game, it)
                    }.show()
                }
            }
        }
        mActionModeCallback?.onDeletePressed = {
            mAdapter?.let { adapter ->
                Dialogs.deleteGamesDialog(activity) {
                    removeGames(adapter.selectedItems)
                }.show()
            }
        }
        mActionModeCallback?.onDestroy = { mActionMode = null }
    }

    private fun setupRecyclerView() {
        mAdapter = GameAdapter(realm.where<Game>().findAll())
        mAdapter?.onItemClickListener = { holder, position ->
            if (mActionMode == null) {
                val game = holder.item
                activity.supportFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container,
                                GameFragment.newInstance(game.name),
                                TAG_GAME_FRAGMENT)
                        .addToBackStack(null)
                        .commit()
            } else {
                mAdapter?.toggleItemSelected(position)
                mActionMode?.invalidate()
            }
        }
        mAdapter?.onItemLongClickListener = { holder, position ->
            if (mActionMode == null) {
                mAdapter?.toggleItemSelected(position)
                mActionMode = activity.startActionMode(mActionModeCallback)
                true
            } else false
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            isNestedScrollingEnabled = false
        }
    }

    companion object {

        fun newInstance(): GamesListFragment {
            return GamesListFragment()
        }
    }
}
