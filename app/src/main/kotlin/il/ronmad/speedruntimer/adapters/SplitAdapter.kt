package il.ronmad.speedruntimer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import il.ronmad.speedruntimer.Comparison
import il.ronmad.speedruntimer.databinding.SplitListItemBinding
import il.ronmad.speedruntimer.getFormattedTime
import il.ronmad.speedruntimer.realm.Split
import il.ronmad.speedruntimer.realm.calculateSplitTime

private typealias SplitViewHolder = BaseRecyclerViewAdapter.BaseViewHolder<Split, SplitListItemBinding>

class SplitAdapter(splits: List<Split>, comparison: Comparison = Comparison.PERSONAL_BEST) :
    BaseRecyclerViewAdapter<Split, SplitListItemBinding>(splits) {

    var comparison: Comparison = comparison
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SplitViewHolder(SplitListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: SplitViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.apply {
            val split = item
            viewBinding.nameText.text = split.name
            viewBinding.segmentDurationText.text = when (comparison) {
                Comparison.PERSONAL_BEST -> split.pbTime.getFormattedTime(dashIfZero = true)
                Comparison.BEST_SEGMENTS -> split.bestTime.getFormattedTime(dashIfZero = true)
            }
            viewBinding.splitTimeText.text = split.calculateSplitTime(comparison).getFormattedTime(dashIfZero = true)
        }
    }

    override fun onItemMoved(oldPos: Int, newPos: Int) {
        notifyItemRangeChanged(oldPos, newPos)
    }

    override fun onItemEdited(position: Int) {
        notifyItemRangeChanged(position, itemCount - position)
    }
}
