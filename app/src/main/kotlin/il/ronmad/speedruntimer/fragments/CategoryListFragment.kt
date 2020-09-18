package il.ronmad.speedruntimer.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ActionMode
import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import il.ronmad.speedruntimer.*
import il.ronmad.speedruntimer.adapters.CategoryAdapter
import il.ronmad.speedruntimer.realm.*
import kotlinx.android.synthetic.main.fragment_category_list.*

class CategoryListFragment : BaseFragment(R.layout.fragment_category_list) {

    private lateinit var game: Game
    private var selectedCategory: Category? = null
    private var mAdapter: CategoryAdapter? = null
    var mActionMode: ActionMode? = null
    private var mActionModeCallback: MyActionModeCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val gameName = it.getString(ARG_GAME_NAME)!!
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
        mAdapter?.onItemsEdited()
        if (waitingForTimerPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !TimerService.IS_ACTIVE) {
                if (Settings.canDrawOverlays(context)) {
                    launchTimer()
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
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                        || Settings.canDrawOverlays(context)) {
                    launchTimer()
                }
            }
        }
    }

    private fun launchTimer() {
        waitingForTimerPermission = false
        try {
            checkNotNull(selectedCategory)
            TimerService.launchTimer(context, game.name, selectedCategory!!.name)
        } catch (e: IllegalStateException) { /* selectedCategory was null */
        }
    }

    private fun checkEmptyList() {
        emptyList?.visibility = if (game.categories.size == 0) View.VISIBLE else View.GONE
    }

    private fun setupActionMode() {
        mActionModeCallback = MyActionModeCallback(mAdapter!!).apply {
            onEditPressed = {
                mAdapter?.selectedItems?.singleOrNull()?.let { id ->
                    game.getCategoryById(id)?.let {
                        Dialogs.showEditCategoryDialog(activity, it) { name, pbTime, runCount ->
                            actionEditCategory(it, name, pbTime, runCount)
                        }
                    }
                }
            }
            onDeletePressed = {
                mAdapter?.let {
                    actionRemoveCategories(it.selectedItems)
                }
            }
            onDestroy = { mActionMode = null }
        }
    }

    private fun setupRecyclerView() {
        mAdapter = CategoryAdapter(activity, game.categories).apply {
            onItemClickListener = { holder, position ->
                if (mActionMode == null) {
                    selectedCategory = holder.item
                    showBottomSheetDialog()
                } else {
                    mAdapter?.toggleItemSelected(position)
                    mActionMode?.invalidate()
                }
            }
            onItemLongClickListener = { _, position ->
                if (mActionMode == null) {
                    mAdapter?.toggleItemSelected(position)
                    mActionMode = activity.startActionMode(mActionModeCallback)
                    true
                } else false
            }
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            ViewCompat.setNestedScrollingEnabled(this, false)
        }
    }

    override fun onFabAddPressed() {
        Dialogs.showNewCategoryDialog(activity, game) {
            addCategory(it)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun checkPermissionAndStartTimer() {
        context?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !Settings.canDrawOverlays(it)) {
                waitingForTimerPermission = true
                it.showToast(it.getString(R.string.toast_allow_permission), 1)
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${activity.packageName}"))
                startActivityForResult(intent, OVERLAY_REQUEST_CODE, Bundle())
            } else {
                launchTimer()
            }
        }
    }

    /**
     * All of this is because the permission may take time to register.
     */
    private fun checkPermissionAndStartTimerDelayed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        val handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
        handler.postDelayed({
            if (Settings.canDrawOverlays(context)) {
                launchTimer()
            } else {
                handler.postDelayed({
                    if (Settings.canDrawOverlays(context)) {
                        launchTimer()
                    } else {
                        handler.postDelayed({
                            if (Settings.canDrawOverlays(context)) {
                                launchTimer()
                            }
                        }, 500)
                    }
                }, 500)
            }
        }, 500)
    }

    private fun addCategory(name: String) {
        game.addCategory(name)
        mAdapter?.onItemAdded()
        checkEmptyList()
        mActionMode?.finish()
    }

    private fun editCategory(category: Category, newName: String, newBestTime: Long, newRunCount: Int) {
        category.updateData(newName, newBestTime, newRunCount)
        mAdapter?.onItemsEdited()
        mActionMode?.finish()
    }

    private fun removeCategories(toRemove: Collection<Long>) {
        if (toRemove.isEmpty()) return
        game.removeCategories(toRemove)
        mAdapter?.onItemsRemoved()
        checkEmptyList()
        mActionMode?.finish()
    }

    private fun viewSplits() {
        try {
            checkNotNull(selectedCategory)
            activity.supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                            R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragment_container,
                            SplitsFragment.newInstance(game.name, selectedCategory!!.name),
                            TAG_SPLITS_LIST_FRAGMENT)
                    .addToBackStack(null)
                    .commit()
        } catch (e: IllegalStateException) { /* selectedCategory was null */
        }
    }

    private fun showBottomSheetDialog() {
        CategoryBottomSheetFragment.newInstance().apply {
            onViewSplitsClickListener = { viewSplits() }
            onLaunchTimerClickListener = { checkPermissionAndStartTimer() }
            show(this@CategoryListFragment.activity.supportFragmentManager,
                    TAG_CATEGORY_BOTTOM_SHEET_DIALOG)
        }
    }

    private fun actionRemoveCategories(toRemove: Collection<Long>) {
        if (toRemove.isEmpty()) return
        game.getCategories(toRemove).singleOrNull()?.let {
            if (it.bestTime > 0) {
                Dialogs.showDeleteCategoryDialog(activity, it) {
                    removeCategories(toRemove)
                }
            } else {
                removeCategories(toRemove)
            }
        } ?: Dialogs.showDeleteCategoriesDialog(activity) {
            removeCategories(toRemove)
        }
    }

    private fun actionEditCategory(category: Category, newName: String, newBestTime: Long, newRunCount: Int) {
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

        private var waitingForTimerPermission = false
        private const val OVERLAY_REQUEST_CODE = 251

        fun newInstance(gameName: String) = CategoryListFragment().apply {
            arguments = Bundle().also { it.putString(ARG_GAME_NAME, gameName) }
        }
    }
}
