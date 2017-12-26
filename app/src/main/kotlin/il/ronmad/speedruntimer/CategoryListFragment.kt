package il.ronmad.speedruntimer

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.Toast
import io.realm.kotlin.where

class CategoryListFragment : BaseListFragment() {

    private lateinit var game: Game
    private var categoryPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val gameName = it.getString(ARG_GAME_NAME)
            game = realm.where<Game>().equalTo("name", gameName).findFirst()!!
        }
        layoutResId = R.layout.fragment_category_list
        contextMenuResId = R.menu.category_list_fragment_context_menu
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mListAdapter = CategoryAdapter(activity, game.categories)
        listAdapter = mListAdapter
    }

    override fun onResume() {
        super.onResume()
        if (backFromPermissionCheck) {
            backFromPermissionCheck = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !TimerService.IS_ACTIVE) {
                if (Settings.canDrawOverlays(activity)) {
                    startTimerService()
                } else {
                    checkPermissionAndStartTimerDelayed()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            OVERLAY_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(activity)) {
                        startTimerService()
                    }
                } else {
                    startTimerService()
                }
            }
        }
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)

        if (mActionMode == null) {
            categoryPosition = position
            checkPermissionAndStartTimer()
        }
    }

    override fun update() {
        finishActionMode()
        (mListAdapter as CategoryAdapter).data = game.categories
    }

    override fun onMenuEditPressed() {
        val selectedItem = selectedItems[0]
        Dialogs.editCategoryDialog(this, selectedItem as Category).show()
    }

    override fun onMenuDeletePressed() {
        val selectedItems = selectedItems
        actionDeleteCategories(selectedItems.map { it as Category })
    }

    override fun onFabAddPressed() {
        Log.v(TAG_CATEGORY_LIST_FRAGMENT, "onFabAddPressed")
        Dialogs.newCategoryDialog(activity, game).show()
    }

    @SuppressLint("RestrictedApi")
    private fun checkPermissionAndStartTimer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                backFromPermissionCheck = true
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.packageName))
                startActivityForResult(intent, OVERLAY_REQUEST_CODE, Bundle())
            } else {
                startTimerService()
            }
        } else {
            startTimerService()
        }

    }

    /**
     * All of this is because the permission may take time to register.
     */
    private fun checkPermissionAndStartTimerDelayed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        val handler = Handler()
        handler.postDelayed({
            if (Settings.canDrawOverlays(activity)) {
                startTimerService()
            } else {
                handler.postDelayed({
                    if (Settings.canDrawOverlays(activity)) {
                        startTimerService()
                    } else {
                        handler.postDelayed({
                            if (Settings.canDrawOverlays(activity)) {
                                startTimerService()
                            }
                        }, 500)
                    }
                }, 500)
            }
        }, 500)
    }

    private fun startTimerService() {
        if (!tryLaunchGame()) {
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            startActivity(homeIntent)
        }
        if (TimerService.IS_ACTIVE) {
            activity.stopService(Intent(activity, TimerService::class.java))
        }
        val category = (mListAdapter as CategoryAdapter).getItem(categoryPosition)
        val serviceIntent = Intent(activity, TimerService::class.java)
        serviceIntent.putExtra(getString(R.string.extra_game), game.name)
        serviceIntent.putExtra(getString(R.string.extra_category), category.name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(serviceIntent)
        } else {
            activity.startService(serviceIntent)
        }
        TimerService.IS_ACTIVE = true
    }

    private fun tryLaunchGame(): Boolean {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
        if (!sharedPrefs.getBoolean(getString(R.string.key_pref_launch_games), true)) {
            return false
        }
        val game = activity.installedApps.find {
            activity.packageManager.getApplicationLabel(it).toString().toLowerCase() == game.name.toLowerCase()
        }
        game?.let {
            Toast.makeText(activity,
                    "Launching ${activity.packageManager.getApplicationLabel(it)}...", Toast.LENGTH_SHORT).show()
            startActivity(activity.packageManager.getLaunchIntentForPackage(it.packageName))
            return true
        }
        return false
    }

    private fun actionDeleteCategories(toRemove: List<Category>) {
        if (toRemove.size == 1) {
            val category = toRemove[0]
            if (category.bestTime > 0) {
                Dialogs.deleteCategoryDialog(activity, game, toRemove).show()
            } else {
                game.removeCategories(toRemove)
            }
        } else {
            Dialogs.deleteCategoriesDialog(activity, game, toRemove).show()
        }

    }

    fun editCategory(category: Category, newBestTime: Long, newRunCount: Int) {
        val prevBestTime = category.bestTime
        val prevRunCount = category.runCount
        category.setData(newBestTime, newRunCount)
        showEditedCategorySnackbar(category, prevBestTime, prevRunCount)
    }

    private fun showEditedCategorySnackbar(category: Category, prevBestTime: Long, prevRunCount: Int) {
        val message = "${game.name} ${category.name} has been edited."
        Snackbar.make(view!!, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) { category.setData(prevBestTime, prevRunCount) }
                .show()
    }

    companion object {

        private var backFromPermissionCheck = false
        private val OVERLAY_REQUEST_CODE = 251

        private val ARG_GAME_NAME = "game-name"

        fun newInstance(gameName: String): CategoryListFragment {
            val fragment = CategoryListFragment()
            val args = Bundle()
            args.putString(ARG_GAME_NAME, gameName)
            fragment.arguments = args
            return fragment
        }
    }
}
