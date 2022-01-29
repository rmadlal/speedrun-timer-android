package il.ronmad.speedruntimer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import il.ronmad.speedruntimer.activities.MainActivity
import io.realm.Realm

abstract class BaseFragment<T : ViewBinding>(private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> T) : Fragment() {

    private var _viewBinding: T? = null
    internal val viewBinding get() = _viewBinding!!

    protected lateinit var realm: Realm

    protected val activity: MainActivity
        get() = getActivity() as MainActivity

    protected val mActionBar: ActionBar?
        get() = activity.supportActionBar

    protected val fabAdd: FloatingActionButton
        get() = activity.viewBinding.fabAdd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = bindingInflater(inflater, container, false)
        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    abstract fun onFabAddPressed()
}
