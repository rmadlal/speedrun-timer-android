package il.ronmad.speedruntimer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.category_list_item.view.*

class CategoryAdapter(val context: Context, categories: List<Category>)
    : BaseRecyclerViewAdapter<Category>(categories) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            CategoryViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.category_list_item, parent, false))

    override fun onBindViewHolder(holder: BaseViewHolder<Category>, position: Int) {
        super.onBindViewHolder(holder, position)
        (holder as CategoryViewHolder).apply {
            val category = item
            categoryNameText.text = category.name
            pbTime.text = if (category.bestTime > 0) category.bestTime.getFormattedTime() else "None yet"
            pbTime.setTextColor(context.getColorCpt(if (category.bestTime > 0) R.color.colorAccent
            else android.R.color.primary_text_light))
            runsNum.text = category.runCount.toString()
        }
    }

    override fun onItemMoved(oldPos: Int, newPos: Int) {
        notifyItemMoved(oldPos, newPos)
    }

    override fun onItemEdited(position: Int) {
        notifyItemChanged(position)
    }

    class CategoryViewHolder(categoryView: View) : BaseViewHolder<Category>(categoryView) {
        val categoryNameText: TextView = categoryView.categoryName
        val pbTime: TextView = categoryView.pbTime
        val runsNum: TextView = categoryView.runsNum
    }
}
