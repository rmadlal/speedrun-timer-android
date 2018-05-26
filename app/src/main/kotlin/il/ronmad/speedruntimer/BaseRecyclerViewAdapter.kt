package il.ronmad.speedruntimer

import android.support.v7.widget.RecyclerView
import android.view.View
import il.ronmad.speedruntimer.BaseRecyclerViewAdapter.BaseViewHolder

abstract class BaseRecyclerViewAdapter<T: HasPrimaryId>(val data: List<T>)
    : RecyclerView.Adapter<BaseViewHolder<T>>() {

    var onItemClickListener: ((BaseViewHolder<T>, Int) -> Unit)? = null
    var onItemLongClickListener: ((BaseViewHolder<T>, Int) -> Boolean)? = null

    var selectedItems: Set<Long> = setOf()

    val isEmpty = data.isEmpty()

    /* Add this in extending classes
    init {
        setHasStableIds(true)
    }
    */

    fun getItem(position: Int) = data[position]

    operator fun get(position: Int) = getItem(position)

    override fun getItemCount() = data.count()

    override fun getItemId(position: Int) = getItem(position).id

    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        val item = getItem(position)
        holder.item = item

        setItemBackground(holder.mView, position)

        holder.mView.setOnClickListener { onItemClickListener?.invoke(holder, position) }
        holder.mView.setOnLongClickListener { onItemLongClickListener?.invoke(holder, position)
                ?: false
        }
    }

    abstract fun onItemMoved(oldPos: Int, newPos: Int)

    abstract fun onItemEdited(position: Int)

    fun onItemsRemoved() {
        notifyDataSetChanged()
    }

    fun onItemsEdited() {
        notifyDataSetChanged()
    }

    fun onItemAdded(position: Int = itemCount) {
        notifyItemInserted(position)
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

    open class BaseViewHolder<T: Any>(val mView: View) : RecyclerView.ViewHolder(mView) {
        lateinit var item: T
    }
}