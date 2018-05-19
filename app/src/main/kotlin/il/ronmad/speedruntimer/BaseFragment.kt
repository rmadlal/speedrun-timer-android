package il.ronmad.speedruntimer

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*

abstract class BaseFragment : Fragment() {

    protected lateinit var realm: Realm

    protected val activity: MainActivity
        get() = getActivity() as MainActivity

    protected var mActionBar: ActionBar? = null
        get() = activity.supportActionBar

    protected val fabAdd: FloatingActionButton
        get() = activity.fabAdd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realm = Realm.getDefaultInstance()
        setHasOptionsMenu(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    abstract fun onFabAddPressed()
}
