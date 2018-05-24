package il.ronmad.speedruntimer

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.category_list_item.view.*

class CategoryAdapter(context: Context,
                      categories: List<Category>)
    : MyBaseListFragmentAdapter<Category>(context, categories, R.layout.category_list_item),
        TimeExtensions {

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        val listItemView = super.getView(i, view, viewGroup)

        val category = getItem(i)
        listItemView.categoryName.text = category.name
        listItemView.pbTime.text = if (category.bestTime > 0) category.bestTime.getFormattedTime() else "None yet"
        listItemView.pbTime.setTextColor(ContextCompat.getColor(context,
                if (category.bestTime > 0) R.color.colorAccent else android.R.color.primary_text_light))
        listItemView.runsNum.text = category.runCount.toString()

        return listItemView
    }
}
