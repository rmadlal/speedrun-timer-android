package il.ronmad.speedruntimer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import il.ronmad.speedruntimer.databinding.CategoryBottomSheetDialogBinding

class CategoryBottomSheetFragment : BottomSheetDialogFragment() {

    private var _viewBinding: CategoryBottomSheetDialogBinding? = null
    private val viewBinding get() = _viewBinding!!

    var onLaunchTimerClickListener: (() -> Unit)? = null
    var onViewSplitsClickListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = CategoryBottomSheetDialogBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.launchTimerItem.setOnClickListener {
            onLaunchTimerClickListener?.invoke()
            dismissAllowingStateLoss()
        }
        viewBinding.viewSplitsItem.setOnClickListener {
            onViewSplitsClickListener?.invoke()
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }
}
