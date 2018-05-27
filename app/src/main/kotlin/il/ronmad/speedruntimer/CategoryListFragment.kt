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
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.ActionMode
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_category_list.*

class CategoryListFragment : BaseFragment(R.layout.fragment_category_list) {

    private lateinit var game: Game
    private var selectedCategory: Category? = null
    lateinit var mAdapter: CategoryAdapter
    var mActionMode: ActionMode? = null
    private lateinit var mActionModeCallback: MyActionModeCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val gameName = it.getString(ARG_GAME_NAME)
            game = realm.getGameByName(gameName)!!
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupRecyclerView()
        setupActionMode()
        checkEmptyList()

        fabAdd.setOnClickListener { onFabAddPressed() }
    }

    override fun onResume() {
        super.onResume()
        mAdapter.onItemsEdited()
        if (backFromPermissionCheck) {
            backFromPermissionCheck = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !TimerService.IS_ACTIVE) {
                if (Settings.canDrawOverlays(context)) {
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
                    if (Settings.canDrawOverlays(context)) {
                        startTimerService()
                    }
                } else {
                    startTimerService()
                }
            }
        }
    }

    private fun checkEmptyList() {
        emptyList.visibility = if (game.categories.count() == 0) View.VISIBLE else View.GONE
    }

    private fun setupActionMode() {
        mActionModeCallback = MyActionModeCallback(mAdapter)
        mActionModeCallback.onEditPressed = {
            game.getCategoryById(mAdapter.selectedItems.first())?.let {
                Dialogs.editCategoryDialog(activity, it) { name, pbTime, runCount ->
                    actionEditCategory(it, name, pbTime, runCount)
                }.show()
            }
        }
        mActionModeCallback.onDeletePressed = {
            actionRemoveCategories(mAdapter.selectedItems)
        }
        mActionModeCallback.onDestroy = { mActionMode = null }
    }

    private fun setupRecyclerView() {
        mAdapter = CategoryAdapter(activity, game.categories)
        mAdapter.onItemClickListener = { holder, position ->
            if (mActionMode == null) {
                selectedCategory = holder.item
                showBottomSheetDialog()
            } else {
                mAdapter.toggleItemSelected(position)
                mActionMode?.invalidate()
            }
        }
        mAdapter.onItemLongClickListener = { holder, position ->
            if (mActionMode == null) {
                mAdapter.toggleItemSelected(position)
                mActionMode = activity.startActionMode(mActionModeCallback)
                true
            } else false
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            isNestedScrollingEnabled = false
        }
    }

    override fun onFabAddPressed() {
        Dialogs.newCategoryDialog(activity, game) {
            addCategory(it)
        }.show()
    }
    @SuppressLint("RestrictedApi")
    private fun checkPermissionAndStartTimer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                backFromPermissionCheck = true
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${activity.packageName}"))
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
            if (Settings.canDrawOverlays(context)) {
                startTimerService()
            } else {
                handler.postDelayed({
                    if (Settings.canDrawOverlays(context)) {
                        startTimerService()
                    } else {
                        handler.postDelayed({
                            if (Settings.canDrawOverlays(context)) {
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
        val serviceIntent = Intent(activity, TimerService::class.java)
        serviceIntent.putExtra(getString(R.string.extra_game), game.name)
        serviceIntent.putExtra(getString(R.string.extra_category), selectedCategory!!.name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(serviceIntent)
        } else {
            activity.startService(serviceIntent)
        }
        TimerService.IS_ACTIVE = true
    }

    private fun addCategory(name: String) {
        game.addCategory(name)
        mAdapter.onItemAdded()
        checkEmptyList()
        mActionMode?.finish()
    }

    private fun editCategory(category: Category, newName: String, newBestTime: Long, newRunCount: Int) {
        category.updateData(newName, newBestTime, newRunCount)
        mAdapter.onItemsEdited()
        mActionMode?.finish()
    }

    private fun removeCategories(toRemove: Collection<Long>) {
        game.removeCategories(toRemove)
        mAdapter.onItemsRemoved()
        checkEmptyList()
        mActionMode?.finish()
    }

    private fun viewSplits() {
        activity.supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                        R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container,
                        SplitsFragment.newInstance(game.name, selectedCategory!!.name),
                        TAG_SPLITS_LIST_FRAGMENT)
                .addToBackStack(null)
                .commit()
    }

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = CategoryBottomSheetFragment.newInstance()
        bottomSheetDialog.show(activity.supportFragmentManager,
                TAG_CATEGORY_BOTTOM_SHEET_DIALOG)
        bottomSheetDialog.onViewSplitsClickListener = { viewSplits() }
        bottomSheetDialog.onLaunchTimerClickListener = { checkPermissionAndStartTimer() }
    }

    private fun tryLaunchGame(): Boolean {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
        if (!sharedPrefs.getBoolean(getString(R.string.key_pref_launch_games), true)) {
            return false
        }
        activity.installedApps.find {
            activity.packageManager.getApplicationLabel(it).toString().toLowerCase() == game.name.toLowerCase()
        }?.let {
            Toast.makeText(activity,
                    "Launching ${activity.packageManager.getApplicationLabel(it)}...", Toast.LENGTH_SHORT).show()
            startActivity(activity.packageManager.getLaunchIntentForPackage(it.packageName))
            return true
        }
        return false
    }

    private fun actionRemoveCategories(toRemove: Collection<Long>) {
        game.getCategories(toRemove).singleOrNull()?.let {
            if (it.bestTime > 0) {
                Dialogs.deleteCategoryDialog(activity, it) {
                    removeCategories(toRemove)
                }.show()
            } else {
                removeCategories(toRemove)
            }
        } ?: Dialogs.deleteCategoriesDialog(activity) {
            removeCategories(toRemove)
        }.show()
    }

    fun actionEditCategory(category: Category, newName: String, newBestTime: Long, newRunCount: Int) {
        val prevName = category.name
        val prevBestTime = category.bestTime
        val prevRunCount = category.runCount
        editCategory(category, newName, newBestTime, newRunCount)
        showEditedCategorySnackbar(category, prevName, prevBestTime, prevRunCount)
    }

    private fun showEditedCategorySnackbar(category: Category, prevName: String, prevBestTime: Long, prevRunCount: Int) {
        val message = "${game.name} $prevName has been edited."
        Snackbar.make(view!!, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) {
                    editCategory(category, prevName, prevBestTime, prevRunCount)
                }.show()
    }

    companion object {

        private var backFromPermissionCheck = false
        private const val OVERLAY_REQUEST_CODE = 251

        fun newInstance(gameName: String): CategoryListFragment {
            val fragment = CategoryListFragment()
            val args = Bundle()
            args.putString(ARG_GAME_NAME, gameName)
            fragment.arguments = args
            return fragment
        }
    }
}
