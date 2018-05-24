package il.ronmad.speedruntimer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

abstract class MyBaseListFragmentAdapter<T>(val context: Context,
                                            data: List<T>,
                                            private val listItemResourceId: Int) : BaseAdapter() {

    internal var data: List<T> = data
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var checkedItems: List<T> = listOf()

    override fun getCount() = data.size

    override fun getItem(i: Int) = data[i]

    override fun getItemId(i: Int) = i.toLong()

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        val listItemView = view ?:
                LayoutInflater.from(context).inflate(listItemResourceId, viewGroup, false)

        setItemBackground(listItemView, i)
        return listItemView
    }

    internal fun setItemChecked(i: Int, checked: Boolean) {
        val item = getItem(i)
        if (checked) {
            checkedItems += item
        } else {
            checkedItems -= item
        }
        notifyDataSetChanged()
    }

    internal fun isItemChecked(i: Int) = checkedItems.contains(getItem(i))

    internal fun clearSelections() {
        checkedItems = listOf()
        notifyDataSetChanged()
    }

    private fun setItemBackground(listItemView: View, i: Int) {
        if (isItemChecked(i)) {
            listItemView.setBackgroundResource(R.color.colorHighlightedListItem)
        } else {
            listItemView.setBackgroundResource(android.R.color.transparent)
        }
    }

    operator fun get(position: Int) = this.getItem(position)
}
