package il.ronmad.speedruntimer.adapters

import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

abstract class SmartFragmentStatePagerAdapter(fragmentManager: FragmentManager) :
        FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val registeredFragments = SparseArray<Fragment>()

    override fun instantiateItem(container: ViewGroup, position: Int): Fragment {
        val fragment = super.instantiateItem(container, position) as Fragment
        registeredFragments.put(position, fragment)
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        registeredFragments.remove(position)
        super.destroyItem(container, position, obj)
    }

    fun getRegisteredFragment(position: Int): Fragment? = registeredFragments.get(position)
}
