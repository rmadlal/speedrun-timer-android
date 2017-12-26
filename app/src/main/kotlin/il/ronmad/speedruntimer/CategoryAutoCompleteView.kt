package il.ronmad.speedruntimer

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.AppCompatAutoCompleteTextView
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter

import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class CategoryAutoCompleteView : AppCompatAutoCompleteTextView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    internal fun setCategories(gameName: String) {
        if (gameName in categoryCache) {
            setAdapter(ArrayAdapter(context,
                    R.layout.autocomplete_dropdown_item, categoryCache[gameName]))
        } else {
            launch(UI) {
                val categories = Src.fetchCategoriesForGame(getContext(), gameName)
                val categoryNames = if (categories.isEmpty())
                    listOf("Any%", "100%", "Low%")
                else
                    categories.map { it.name }
                categoryCache += gameName to categoryNames
                setAdapter(ArrayAdapter(getContext(),
                        R.layout.autocomplete_dropdown_item, categoryNames))
                if (isShown) {
                    showDropDown()
                }
            }

        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)

        if (windowVisibility != View.VISIBLE) {
            return
        }
        if (focused) {
            if (error == null) {
                showDropDown()
            }
        } else {
            dismissDropDown()
        }
    }

    override fun enoughToFilter() = true

    companion object {

        private var categoryCache: Map<String, List<String>> = mapOf()
    }
}
