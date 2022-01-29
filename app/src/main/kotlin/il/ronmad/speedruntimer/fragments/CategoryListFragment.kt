package il.ronmad.speedruntimer.fragments

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ActionMode
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import il.ronmad.speedruntimer.*
import il.ronmad.speedruntimer.adapters.CategoryAdapter
import il.ronmad.speedruntimer.databinding.FragmentCategoryListBinding
import il.ronmad.speedruntimer.realm.*

class CategoryListFragment : BaseFragment<FragmentCategoryListBinding>(FragmentCategoryListBinding::inflate) {

    private lateinit var game: Game
    private var selectedCategory: Category? = null
    private var mAdapter: CategoryAdapter? = null
    var mActionMode: ActionMode? = null
    private var mActionModeCallback: MyActionModeCallback? = null
    private lateinit var getOverlayPermissionAndLaunchTimer: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gameName = requireArguments().getString(ARG_GAME_NAME)!!
        game = realm.getGameByName(gameName)!!

        getOverlayPermissionAndLaunchTimer =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (Settings.canDrawOverlays(context)) {
                    launchTimer()
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupActionMode()
        checkEmptyList()

        fabAdd.setOnClickListener { onFabAddPressed() }
    }

    override fun onResume() {
        super.onResume()
        mAdapter?.onItemsEdited()
        if (waitingForTimerPermission) {
            if (!TimerService.IS_ACTIVE) {
                if (Settings.canDrawOverlays(context)) {
                    launchTimer()
                } else {
                    checkPermissionAndStartTimerDelayed()
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
        viewBinding.emptyList.visibility = if (game.categories.size == 0) View.VISIBLE else View.GONE
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
        viewBinding.recyclerView.apply {
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

    private fun checkPermissionAndStartTimer() {
        context?.let {
            if (!Settings.canDrawOverlays(it)) {
                waitingForTimerPermission = true
                it.showToast(it.getString(R.string.toast_allow_permission), 1)
                getOverlayPermissionAndLaunchTimer.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${activity.packageName}")
                    )
                )
            } else {
                launchTimer()
            }
        }
    }

    /**
     * All of this is because the permission may take time to register.
     */
    private fun checkPermissionAndStartTimerDelayed() {
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
        CategoryBottomSheetFragment().also {
            it.onViewSplitsClickListener = ::viewSplits
            it.onLaunchTimerClickListener = ::checkPermissionAndStartTimer
            it.show(activity.supportFragmentManager, TAG_CATEGORY_BOTTOM_SHEET_DIALOG)
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
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo) {
                    editCategory(category, prevName, prevBestTime, prevRunCount)
                }.show()
    }

    companion object {

        private var waitingForTimerPermission = false

        fun newInstance(gameName: String) = CategoryListFragment().apply {
            arguments = Bundle().also { it.putString(ARG_GAME_NAME, gameName) }
        }
    }
}
