package il.ronmad.speedruntimer

import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.view.*
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_game.*

class GameFragment : Fragment() {

    private lateinit var realm: Realm
    private lateinit var game: Game

    private lateinit var viewPagerAdapter: SmartFragmentStatePagerAdapter

    private val activity: MainActivity
        get() = getActivity() as MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        realm = Realm.getDefaultInstance()
        arguments?.let {
            val gameName = it.getString(ARG_GAME_NAME)
            game = realm.where<Game>().equalTo("name", gameName).findFirst()!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_game, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity.setSupportActionBar(toolbar)
        // Set toolbar elevation to 4dp
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val scale = resources.displayMetrics.density
            appBarLayout.elevation = (4 * scale + 0.5f).toInt().toFloat()
        }
        activity.supportActionBar?.title = game.name
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupViewPager()
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
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
