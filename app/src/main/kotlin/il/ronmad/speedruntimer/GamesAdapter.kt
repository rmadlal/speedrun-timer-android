package il.ronmad.speedruntimer

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.games_list_item.view.*

class GamesAdapter(context: Context, games: List<Game>)
    : MyBaseListFragmentAdapter<Game>(context, games, R.layout.games_list_item) {

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        val listItemView = super.getView(i, view, viewGroup)

        listItemView.gameName.text = getItem(i).name
        return listItemView
    }
}
