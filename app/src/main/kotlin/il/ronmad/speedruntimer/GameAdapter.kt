package il.ronmad.speedruntimer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.games_list_item.view.*


class GameAdapter(games: List<Game>) : BaseRecyclerViewAdapter<Game>(games) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            GameViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.games_list_item, parent, false))

    override fun onBindViewHolder(holder: BaseViewHolder<Game>, position: Int) {
        super.onBindViewHolder(holder, position)
        (holder as GameViewHolder).apply {
            val game = item
            gameNameText.text = game.name
        }
    }

    override fun onItemMoved(oldPos: Int, newPos: Int) {
        notifyItemMoved(oldPos, newPos)
    }

    override fun onItemEdited(position: Int) {
        notifyItemChanged(position)
    }

    class GameViewHolder(gameView: View) : BaseViewHolder<Game>(gameView) {
        val gameNameText: TextView = gameView.gameName
    }
}
