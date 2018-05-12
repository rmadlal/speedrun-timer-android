package il.ronmad.speedruntimer

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ListView
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_games_list.*

class GamesListFragment : BaseListFragment<Game>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutResId = R.layout.fragment_games_list
        contextMenuResId = R.menu.games_list_fragment_context_menu
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity.setSupportActionBar(toolbar)
        // Set toolbar elevation to 4dp
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val scale = resources.displayMetrics.density
            appBarLayout.elevation = (4 * scale + 0.5f).toInt().toFloat()
        }
        activity.supportActionBar?.title = activity.getString(R.string.app_name)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        activity.fabAdd.show()
        mListAdapter = GamesAdapter(activity, realm.where<Game>().findAll())
        listAdapter = mListAdapter
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)

        if (mActionMode == null) {
            val game = mListAdapter[position]
            fragmentManager?.beginTransaction()
                    ?.setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                            R.anim.fade_in, R.anim.fade_out)
                    ?.replace(R.id.fragment_container, GameFragment.newInstance(game.name), TAG_GAME_FRAGMENT)
                    ?.addToBackStack(null)
                    ?.commit()
        }
    }

    override fun update() {
        finishActionMode()
        mListAdapter.data = realm.where<Game>().findAll()
    }

    override fun onMenuEditPressed() {
        if (selectedItems.isEmpty()) return
        val selectedItem = selectedItems[0]
        Dialogs.editGameDialog(activity, realm, selectedItem).show()
    }

    override fun onMenuDeletePressed() {
        if (selectedItems.isEmpty()) return
        actionDeleteGames(selectedItems)
    }

    override fun onFabAddPressed() {
        Dialogs.newGameDialog(activity, realm).show()
    }

    private fun actionDeleteGames(toRemove: List<Game>) {
        Dialogs.deleteGamesDialog(activity, realm, toRemove).show()
    }

    companion object {

        fun newInstance(): GamesListFragment {
            return GamesListFragment()
        }
    }
}
