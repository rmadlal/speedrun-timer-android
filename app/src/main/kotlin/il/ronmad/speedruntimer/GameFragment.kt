package il.ronmad.speedruntimer

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.*
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_game.*

class GameFragment : BaseFragment() {

    private lateinit var game: Game
    private lateinit var viewPagerAdapter: SmartFragmentStatePagerAdapter
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            val gameName = it.getString(ARG_GAME_NAME)
            game = realm.where<Game>().equalTo("name", gameName).findFirst()!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_game, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActionBar?.title = game.name
        mActionBar?.setDisplayHomeAsUpEnabled(true)

        tabLayout = activity.tabLayout
        setupViewPager()
        tabLayout.visibility = View.VISIBLE

        fabAdd.setOnClickListener { onFabAddPressed() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity.tabLayout.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            return when (it.itemId) {
                android.R.id.home -> {
                    activity.onBackPressed()
                    true
                }
                else -> false
            }
        }
        return false
    }

    override fun onFabAddPressed() {
        Dialogs.newCategoryDialog(activity, game) { game.addCategory(it) }.show()
    }

    private fun setupViewPager() {
        viewPagerAdapter = object : SmartFragmentStatePagerAdapter(childFragmentManager) {
            override fun instantiateItem(container: ViewGroup, position: Int): Fragment {
                val fragment = super.instantiateItem(container, position)
                if (fragment is GameInfoFragment && viewPager.currentItem == TAB_INFO) {
                    fragment.refreshData()
                }
                return fragment
            }

            override fun getItem(position: Int): Fragment {
                return when (position) {
                    TAB_CATEGORIES -> CategoryListFragment.newInstance(game.name)
                    TAB_INFO -> GameInfoFragment.newInstance(game.name)
                    else -> CategoryListFragment.newInstance(game.name)
                }
            }

            override fun getPageTitle(position: Int): CharSequence? =
                    resources.getStringArray(R.array.fragment_game_tabs)[position]

            override fun getCount() = 2
        }
        viewPager.adapter = viewPagerAdapter
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                when (position) {
                    TAB_CATEGORIES -> activity.fabAdd.show()
                    TAB_INFO -> {
                        activity.fabAdd.hide()
                        (viewPagerAdapter.getRegisteredFragment(TAB_INFO) as? GameInfoFragment)
                                ?.let {
                                    if (!it.isDataShowing) {
                                        it.refreshData()
                                    }
                                }
                        (viewPagerAdapter.getRegisteredFragment(TAB_CATEGORIES) as? CategoryListFragment)
                                ?.finishActionMode()
                    }
                }
            }
        })

        tabLayout.setupWithViewPager(viewPager)
    }

    companion object {

        const val TAB_CATEGORIES = 0
        const val TAB_INFO = 1

        fun newInstance(gameName: String): GameFragment {
            val fragment = GameFragment()
            val args = Bundle()
            args.putString(ARG_GAME_NAME, gameName)
            fragment.arguments = args
            return fragment
        }
    }
}
