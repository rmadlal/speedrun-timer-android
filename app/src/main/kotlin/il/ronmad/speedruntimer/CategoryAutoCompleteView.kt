package il.ronmad.speedruntimer

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import com.google.common.collect.Lists
import il.ronmad.speedruntimer.web.Failure
import il.ronmad.speedruntimer.web.Src
import il.ronmad.speedruntimer.web.Success
import kotlinx.coroutines.*
import java.io.IOException

class CategoryAutoCompleteView : AppCompatAutoCompleteTextView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var setCategoriesJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private var categoryNames: List<String> = emptyList()
    private val defaultCategories: List<String>
        get() = listOf("Any%", "100%", "Low%")


    internal fun setCategories(gameName: String) {
        setCategoriesJob = scope.launch {
            try {
                val game = withContext(Dispatchers.IO) {
                    Src().fetchGameData(gameName)
                }
                categoryNames = when (game) {
                    is Success -> {
                        game.value.categories.flatMap { category ->
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
                    }
                    is Failure -> defaultCategories
                }
            } catch (e: IOException) {
                categoryNames = defaultCategories
            } catch (e: OutOfMemoryError) {
                categoryNames = defaultCategories
            } finally {
                if (isActive) {
                    setAdapter(
                        ArrayAdapter(
                            context,
                            R.layout.autocomplete_dropdown_item, categoryNames
                        )
                    )
                    delay(200)  // Weird stuff happens without this.
                    if (isShown) showDropDown()
                }
                setCategoriesJob = null
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        setCategoriesJob?.cancel()
        setCategoriesJob = null
    }
}
