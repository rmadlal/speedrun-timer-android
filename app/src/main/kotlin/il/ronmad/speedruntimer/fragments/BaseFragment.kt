package il.ronmad.speedruntimer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import il.ronmad.speedruntimer.activities.MainActivity
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*

abstract class BaseFragment(private val layoutResId: Int) : Fragment() {

    protected lateinit var realm: Realm

    protected val activity: MainActivity
        get() = getActivity() as MainActivity

    protected val mActionBar: ActionBar?
        get() = activity.supportActionBar

    protected val fabAdd: FloatingActionButton
        get() = activity.fabAdd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutResId, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    abstract fun onFabAddPressed()
}
