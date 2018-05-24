package il.ronmad.speedruntimer

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class SplitPositionSpinnerAdapter(context: Context, numOfElements: Int) : ArrayAdapter<Int>(
        context, android.R.layout.simple_spinner_item, (1..numOfElements).toList()) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = super.getView(position, convertView, parent)
        (view as TextView).gravity = Gravity.CENTER
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = super.getDropDownView(position, convertView, parent)
        (view as TextView).gravity = Gravity.CENTER
        return view
    }
}
