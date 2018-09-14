package il.ronmad.speedruntimer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import il.ronmad.speedruntimer.Comparison
import il.ronmad.speedruntimer.R
import il.ronmad.speedruntimer.getFormattedTime
import il.ronmad.speedruntimer.realm.Split
import il.ronmad.speedruntimer.realm.calculateSplitTime
import kotlinx.android.synthetic.main.split_list_item.view.*

class SplitAdapter(splits: List<Split>,
                   comparison: Comparison = Comparison.PERSONAL_BEST)
    : BaseRecyclerViewAdapter<Split>(splits) {

    var comparison: Comparison = comparison
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            SplitViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.split_list_item, parent, false))

    override fun onBindViewHolder(holder: BaseViewHolder<Split>, position: Int) {
        super.onBindViewHolder(holder, position)
        (holder as SplitViewHolder).apply {
            val split = item
            splitNameText.text = split.name
            segmentDurationText.text = when (comparison) {
                Comparison.PERSONAL_BEST -> split.pbTime.getFormattedTime(dashIfZero = true)
                Comparison.BEST_SEGMENTS -> split.bestTime.getFormattedTime(dashIfZero = true)
            }
            splitTimeText.text = split.calculateSplitTime(comparison).getFormattedTime(dashIfZero = true)
        }
    }

    override fun onItemMoved(oldPos: Int, newPos: Int) {
        notifyItemRangeChanged(oldPos, newPos)
    }

    override fun onItemEdited(position: Int) {
        notifyItemRangeChanged(position, itemCount - position)
    }

    class SplitViewHolder(splitView: View) : BaseViewHolder<Split>(splitView) {
        val splitNameText: TextView = splitView.nameText
        val segmentDurationText: TextView = splitView.segmentDurationText
        val splitTimeText: TextView = splitView.splitTimeText
    }
}
