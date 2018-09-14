package il.ronmad.speedruntimer

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.AppCompatAutoCompleteTextView
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter
import com.google.common.collect.Lists
import il.ronmad.speedruntimer.web.SrcGame
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class CategoryAutoCompleteView : AppCompatAutoCompleteTextView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val defaultCategories: List<String>
        get() = listOf("Any%", "100%", "Low%")

    internal fun setCategories(gameName: String) {
        launch(UI) {
            val app = context?.applicationContext as? MyApplication ?: return@launch
            val game = app.srcApi.fetchGameData(context, gameName)
            val categoryNames = if (game == SrcGame.EMPTY_GAME) defaultCategories
            else try {
                game.categories.flatMap { category ->
                    if (category.subCategories.isEmpty())
                        listOf(category.name)
                    else {
                        val subcategories = category.subCategories.map { srcVariable ->
                            srcVariable.values.map { it.label }
                        }
                        try {
                            Lists.cartesianProduct(subcategories).map {
                                "${category.name} - ${it.joinToString(" ")}"
                            }
                        } catch (e: IllegalArgumentException) {
                            listOf(category.name)
                        }
                    }
                }
            } catch (e: OutOfMemoryError) { defaultCategories }
            setAdapter(ArrayAdapter(context,
                    R.layout.autocomplete_dropdown_item, categoryNames))
            postDelayed({ if (isShown) showDropDown() }, 200)
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
}
