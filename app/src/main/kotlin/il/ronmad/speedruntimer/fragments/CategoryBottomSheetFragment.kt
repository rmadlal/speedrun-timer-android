package il.ronmad.speedruntimer.fragments

import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import il.ronmad.speedruntimer.R
import kotlinx.android.synthetic.main.category_bottom_sheet_dialog.view.*

class CategoryBottomSheetFragment : BottomSheetDialogFragment() {

    var onLaunchTimerClickListener: (() -> Unit)? = null
    var onViewSplitsClickListener: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.category_bottom_sheet_dialog, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        view?.apply {
            launchTimerItem.setOnClickListener {
                onLaunchTimerClickListener?.invoke()
                dismissAllowingStateLoss()
            }
            viewSplitsItem.setOnClickListener {
                onViewSplitsClickListener?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }

    companion object {
        fun newInstance() = CategoryBottomSheetFragment()
    }
}
