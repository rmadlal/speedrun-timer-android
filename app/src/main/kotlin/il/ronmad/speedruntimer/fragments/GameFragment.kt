package il.ronmad.speedruntimer.fragments

import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.*
import il.ronmad.speedruntimer.ARG_GAME_NAME
import il.ronmad.speedruntimer.R
import il.ronmad.speedruntimer.adapters.SmartFragmentStatePagerAdapter
import il.ronmad.speedruntimer.databinding.FragmentGameBinding
import il.ronmad.speedruntimer.realm.Game
import il.ronmad.speedruntimer.realm.getGameByName

class GameFragment : BaseFragment<FragmentGameBinding>(FragmentGameBinding::inflate) {

    private lateinit var game: Game
    private lateinit var viewPagerAdapter: SmartFragmentStatePagerAdapter

    private val viewPager get() = viewBinding.viewPager
    private val tabLayout get() = activity.viewBinding.tabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gameName = requireArguments().getString(ARG_GAME_NAME)!!
        game = realm.getGameByName(gameName)!!

        mActionBar?.apply {
            title = game.name
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.requestFocus()

        setupViewPager()
        tabLayout.visibility = View.VISIBLE

        fabAdd.setOnClickListener { onFabAddPressed() }
    }

    override fun onResume() {
        super.onResume()
        (activity.viewBinding.toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags =
            SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS or SCROLL_FLAG_SNAP
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tabLayout.visibility = View.GONE
        (activity.viewBinding.toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags = 0
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity.onBackPressed()
                true
            }
            else -> false
        }
    }

    override fun onFabAddPressed() { /* Handled in CategoryListFragment */
    }

    private fun setupViewPager() {
        viewPagerAdapter = object : SmartFragmentStatePagerAdapter(childFragmentManager) {
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
                    TAB_CATEGORIES -> {
                        fabAdd.show()
                        view?.setOnKeyListener(null)
                    }
                    TAB_INFO -> {
                        fabAdd.hide()
                        (viewPagerAdapter.getRegisteredFragment(TAB_INFO) as? GameInfoFragment)
                            ?.let {
                                if (!it.isDataShowing) {
                                    it.refreshData()
                                }
                            }
                        (viewPagerAdapter.getRegisteredFragment(TAB_CATEGORIES) as? CategoryListFragment)
                            ?.mActionMode?.finish()
                        view?.setOnKeyListener { _, keyCode, _ ->
                            when (keyCode) {
                                KeyEvent.KEYCODE_BACK -> {
                                    viewPager.currentItem = TAB_CATEGORIES
                                    true
                                }
                                else -> false
                            }
                        }
                    }
                }
            }
        })

        tabLayout.setupWithViewPager(viewPager)
    }

    companion object {

        const val TAB_CATEGORIES = 0
        const val TAB_INFO = 1

        fun newInstance(gameName: String) = GameFragment().apply {
            arguments = Bundle().also { it.putString(ARG_GAME_NAME, gameName) }
        }
    }
}
