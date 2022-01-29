package il.ronmad.speedruntimer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import il.ronmad.speedruntimer.databinding.GamesListItemBinding
import il.ronmad.speedruntimer.realm.Game

private typealias GameViewHolder = BaseRecyclerViewAdapter.BaseViewHolder<Game, GamesListItemBinding>

class GameAdapter(games: List<Game>) : BaseRecyclerViewAdapter<Game, GamesListItemBinding>(games) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        GameViewHolder(GamesListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.viewBinding.gameName.text = holder.item.name
    }

    override fun onItemMoved(oldPos: Int, newPos: Int) {
        notifyItemMoved(oldPos, newPos)
    }

    override fun onItemEdited(position: Int) {
        notifyItemChanged(position)
    }
}
