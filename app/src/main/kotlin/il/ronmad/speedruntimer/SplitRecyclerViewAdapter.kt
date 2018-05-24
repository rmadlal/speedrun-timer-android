package il.ronmad.speedruntimer

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.split_list_item.view.*

class SplitRecyclerViewAdapter(val data: List<Split>,
                               comparison: Comparison = Comparison.PERSONAL_BEST,
                               val onItemClickListener: (SplitViewHolder, Int) -> Unit,
                               val onItemLongClickListener: (SplitViewHolder, Int) -> Boolean)
    : RecyclerView.Adapter<SplitRecyclerViewAdapter.SplitViewHolder>(), TimeExtensions {

    var comparison: Comparison = comparison
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selectedItems: Set<Long> = setOf()

    init {
        setHasStableIds(true)
    }

    private fun getItem(position: Int) = data[position]

    operator fun get(position: Int) = getItem(position)

    override fun getItemCount() = data.count()

    override fun getItemId(position: Int) = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            SplitViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.split_list_item, parent, false))

    override fun onBindViewHolder(holder: SplitViewHolder, position: Int) {
        val split = getItem(position)
        holder.split = split
        holder.splitNameText.text = split.name
        holder.segmentDurationText.text = when (comparison) {
            Comparison.PERSONAL_BEST -> split.pbTime.getFormattedTime(dashIfZero = true)
            Comparison.BEST_SEGMENTS -> split.bestTime.getFormattedTime(dashIfZero = true)
        }
        holder.splitTimeText.text = split.calculateSplitTime(comparison).getFormattedTime(dashIfZero = true)

        setItemBackground(holder.splitView, position)

        holder.splitView.setOnClickListener { onItemClickListener(holder, position) }
        holder.splitView.setOnLongClickListener { onItemLongClickListener(holder, position) }
    }

    fun onItemsRemoved() {
        notifyDataSetChanged()
    }

    fun onItemsEdited() {
        notifyDataSetChanged()
    }

    fun onItemMoved(oldPos: Int, newPos: Int) {
        notifyItemRangeChanged(oldPos, newPos)
    }

    fun onItemAdded(position: Int) {
        notifyItemInserted(position)
    }

    fun onItemEdited(position: Int) {
        notifyItemRangeChanged(position, itemCount - position)
    }

    fun toggleItemSelected(position: Int) {
        selectedItems = if (isItemSelected(position)) selectedItems - getItemId(position)
        else selectedItems + getItemId(position)
        onItemEdited(position)
    }

    fun isItemSelected(position: Int) = selectedItems.contains(getItemId(position))

    fun clearSelections() {
        selectedItems = setOf()
        notifyDataSetChanged()
    }

    private fun setItemBackground(splitView: View, position: Int) {
        splitView.setBackgroundResource(
                if (isItemSelected(position)) R.color.colorHighlightedListItem
                else android.R.color.transparent)
    }

    class SplitViewHolder(val splitView: View) : RecyclerView.ViewHolder(splitView) {
        lateinit var split: Split
        val splitNameText: TextView = splitView.nameText
        val segmentDurationText: TextView = splitView.segmentDurationText
        val splitTimeText: TextView = splitView.splitTimeText
    }
}
