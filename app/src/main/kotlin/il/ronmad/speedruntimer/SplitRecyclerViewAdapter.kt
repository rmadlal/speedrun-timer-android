package il.ronmad.speedruntimer

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.realm.OrderedRealmCollection
import io.realm.RealmRecyclerViewAdapter
import kotlinx.android.synthetic.main.split_list_item.view.*

class SplitRecyclerViewAdapter(data: OrderedRealmCollection<Split>,
                               var comparison: Comparison = Comparison.PERSONAL_BEST)
    : RealmRecyclerViewAdapter<Split, SplitRecyclerViewAdapter.SplitViewHolder>(data, false) {

    var selectedItems: Set<Int> = setOf()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)!!.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            SplitViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.split_list_item, parent, false))

    override fun onBindViewHolder(holder: SplitViewHolder, position: Int) {
        val split = getItem(position) ?: return
        holder.split = split
        holder.splitNameText.text = split.name
        holder.segmentDurationText.text = when (comparison) {
            Comparison.PERSONAL_BEST -> split.pbTime.getFormattedTime(dashIfZero = true)
            Comparison.BEST_SEGMENTS -> split.bestTime.getFormattedTime(dashIfZero = true)
        }
        holder.splitTimeText.text = split.calculateSplitTime(comparison).getFormattedTime(dashIfZero = true)
        setItemBackground(holder.splitView, position)
    }

    fun onItemMoved(oldPos: Int, newPos: Int) {
        getItem(oldPos)?.moveToPosition(newPos)
    }

    fun onItemSwiped(pos: Int) {
        getItem(pos)?.remove()
    }

    fun onComparisonChanged(comparison: Comparison) {
        this.comparison = comparison
        notifyDataSetChanged()
    }

    class SplitViewHolder(val splitView: View) : RecyclerView.ViewHolder(splitView) {
        lateinit var split: Split
        val splitNameText: TextView = splitView.nameText
        val segmentDurationText: TextView = splitView.segmentDurationText
        val splitTimeText: TextView = splitView.splitTimeText
    }

    private fun setItemBackground(splitView: View, position: Int) {
        if (selectedItems.contains(position)) {
            splitView.setBackgroundResource(R.color.colorHighlightedListItem)
        } else {
            splitView.setBackgroundResource(android.R.color.transparent)
        }
    }
}