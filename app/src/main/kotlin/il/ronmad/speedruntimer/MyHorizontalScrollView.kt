package il.ronmad.speedruntimer

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.HorizontalScrollView

class MyHorizontalScrollView : HorizontalScrollView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun requestChildRectangleOnScreen(child: View?, rectangle: Rect?, immediate: Boolean): Boolean {
        return true
    }
}
