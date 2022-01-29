package il.ronmad.speedruntimer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import il.ronmad.speedruntimer.R
import il.ronmad.speedruntimer.databinding.CategoryListItemBinding
import il.ronmad.speedruntimer.getColorCpt
import il.ronmad.speedruntimer.getFormattedTime
import il.ronmad.speedruntimer.realm.Category

private typealias CategoryViewHolder = BaseRecyclerViewAdapter.BaseViewHolder<Category, CategoryListItemBinding>

class CategoryAdapter(val context: Context, categories: List<Category>) :
    BaseRecyclerViewAdapter<Category, CategoryListItemBinding>(categories) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CategoryViewHolder(CategoryListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.apply {
            val category = item
            viewBinding.categoryName.text = category.name
            viewBinding.pbTime.text = if (category.bestTime > 0) category.bestTime.getFormattedTime() else "None yet"
            viewBinding.pbTime.setTextColor(
                context.getColorCpt(
                    if (category.bestTime > 0) R.color.colorAccent
                    else android.R.color.primary_text_light
                )
            )
            viewBinding.runsNum.text = category.runCount.toString()
        }
    }

    override fun onItemMoved(oldPos: Int, newPos: Int) {
        notifyItemMoved(oldPos, newPos)
    }

    override fun onItemEdited(position: Int) {
        notifyItemChanged(position)
    }
}
