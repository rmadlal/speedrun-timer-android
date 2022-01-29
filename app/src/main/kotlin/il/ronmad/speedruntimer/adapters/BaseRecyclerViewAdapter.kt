package il.ronmad.speedruntimer.adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import il.ronmad.speedruntimer.R
import il.ronmad.speedruntimer.adapters.BaseRecyclerViewAdapter.BaseViewHolder
import il.ronmad.speedruntimer.realm.HasPrimaryId

abstract class BaseRecyclerViewAdapter<T : HasPrimaryId, V : ViewBinding>(private val data: List<T>) :
    RecyclerView.Adapter<BaseViewHolder<T, V>>() {

    var onItemClickListener: ((BaseViewHolder<T, V>, Int) -> Unit)? = null
    var onItemLongClickListener: ((BaseViewHolder<T, V>, Int) -> Boolean)? = null

    var selectedItems: Set<Long> = setOf()

    val isEmpty = data.isEmpty()

    /* Add this in extending classes
    init {
        setHasStableIds(true)
    }
    */

    private fun getItem(position: Int) = data[position]

    operator fun get(position: Int) = getItem(position)

    override fun getItemCount() = data.size

    override fun getItemId(position: Int) = getItem(position).id

    override fun onBindViewHolder(holder: BaseViewHolder<T, V>, position: Int) {
        val item = getItem(position)
        holder.item = item

        setItemBackground(holder.viewBinding.root, position)

        holder.viewBinding.root.setOnClickListener { onItemClickListener?.invoke(holder, position) }
        holder.viewBinding.root.setOnLongClickListener {
            onItemLongClickListener?.invoke(holder, position) ?: false
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

    private fun isItemSelected(position: Int) = selectedItems.contains(getItemId(position))

    fun clearSelections() {
        selectedItems = setOf()
        notifyDataSetChanged()
    }

    private fun setItemBackground(splitView: View, position: Int) {
        splitView.setBackgroundResource(
            if (isItemSelected(position)) R.color.colorHighlightedListItem
            else android.R.color.transparent
        )
    }

    open class BaseViewHolder<T : Any, V : ViewBinding>(val viewBinding: V) :
        RecyclerView.ViewHolder(viewBinding.root) {
        lateinit var item: T
    }
}
