package il.ronmad.speedruntimer;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BaseListFragment.OnListFragmentInteractionListener {

    private static int launchCounter = 0;
    private static boolean backFromPermissionCheck = false;

    private FragmentManager fragmentManager;
    private BaseListFragment currentFragment;
    private String currentFragmentTag = null;

    private BroadcastReceiver receiver;
    private SharedPreferences sharedPrefs;
    private Gson gson;

    private FloatingActionButton fabAdd;
    private Snackbar addSnackbar;
    private Snackbar rateSnackbar;
    private boolean rateSnackbarShown;

    List<Game> games;
    Game currentGame;
    Category currentCategory;

    private static final String TAG_GAMES_LIST_FRAGMENT = "GamesListFragment";
    private static final String TAG_CATEGORY_LIST_FRAGMENT = "CategoryListFragment";

    public enum ListAction {
        CLICK,
        EDIT,
        DELETE
    }

    // defintetly new and improved, not at all ripped off. and withoutt typos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Set toolbar elevation to 4dp
            float scale = getResources().getDisplayMetrics().density;
            toolbar.setElevation((int) (4 * scale + 0.5f));
        }

        fabAdd = findViewById(R.id.fabAdd);
        addSnackbar = Snackbar.make(fabAdd, R.string.fab_add_str, Snackbar.LENGTH_LONG);

        gson = new GsonBuilder().create();
        sharedPrefs = getPreferences(MODE_PRIVATE);
        String savedData = sharedPrefs.getString(getString(R.string.key_games), "");
        if (savedData.isEmpty()) {
            games = new ArrayList<>();
            new Handler().postDelayed(() -> addSnackbar.show(), 1000);
        } else {
            try {
                games = new ArrayList<>(Arrays.asList(
                        gson.fromJson(savedData, Game[].class)));
            } catch (JsonSyntaxException e) {
                games = Util.fromJsonLegacy(savedData);
            }
        }
        if (savedInstanceState != null) {
            String currentGameName = savedInstanceState.getString("currentGame");
            if (currentGameName != null) {
                currentGame = getGame(currentGameName);
            } else {
                currentGame = null;
            }
        } else {
            currentGame = null;
        }
        currentCategory = null;

        rateSnackbarShown = sharedPrefs.getBoolean(getString(R.string.key_rate_snackbar_shown), false);
        if (!rateSnackbarShown && launchCounter == 0 && !games.isEmpty()) {
            int savedLaunchCounter = sharedPrefs.getInt(getString(R.string.key_launch_counter), 0);
            launchCounter = Math.min(3, savedLaunchCounter) + 1;
            if (launchCounter == 3) {
                setupRateSnackbar();
                new Handler().postDelayed(() -> rateSnackbar.show(), 1000);
                rateSnackbarShown = true;
            }
        }

        setupReceiver();
        setupFragments();
    }

    /**
     * Deal with the case that the overlay permission didn't register yet.
     * Show a dialog if opening the app when the timer is active.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (backFromPermissionCheck) {
            backFromPermissionCheck = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !TimerService.IS_ACTIVE) {
                if (Settings.canDrawOverlays(this)) {
                    startTimerService();
                } else {
                    checkPermissionAndStartTimerDelayed();
                }
            }
        } else if (TimerService.IS_ACTIVE) {
            Dialogs.closeTimerOnResumeDialog(this).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, TimerService.class));
        unregisterReceiver(receiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentGame != null) {
            outState.putString("currentGame", currentGame.name);
        }
    }

    @Override
    public void onListFragmentInteraction(ListAction action) {
        switch (action) {
            case CLICK:
                listFragmentClick();
                break;
            case EDIT:
                listFragmentEdit();
                break;
            case DELETE:
                listFragmentDelete();
                break;
            default:

                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case android.R.id.home:
                backToGamesListFragment();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT) && currentFragment.isVisible()) {
            backToGamesListFragment();
        } else {
            super.onBackPressed();
        }
    }

    private void backToGamesListFragment() {
        fragmentManager.popBackStack();
        setActionBarTitleAndUpButton(getString(R.string.app_name), false);
        currentFragment = (BaseListFragment) fragmentManager.findFragmentByTag(TAG_GAMES_LIST_FRAGMENT);
        currentFragmentTag = TAG_GAMES_LIST_FRAGMENT;
    }

    final static int OVERLAY_REQUEST_CODE = 251;
    @SuppressLint("RestrictedApi")
    private void checkPermissionAndStartTimer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                backFromPermissionCheck = true;
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_REQUEST_CODE, new Bundle());
            } else {
                startTimerService();
            }
        } else {
            startTimerService();
        }
    }

    /**
     *     All of this is because the permission may take time to register.
     */
    private void checkPermissionAndStartTimerDelayed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (Settings.canDrawOverlays(MainActivity.this)) {
                startTimerService();
            } else {
                handler.postDelayed(() -> {
                    if (Settings.canDrawOverlays(MainActivity.this)) {
                        startTimerService();
                    } else {
                        handler.postDelayed(() -> {
                            if (Settings.canDrawOverlays(MainActivity.this)) {
                                startTimerService();
                            }
                        }, 500);
                    }
                }, 500);
            }
        }, 500);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case OVERLAY_REQUEST_CODE: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        startTimerService();
                    }
                } else {
                    startTimerService();
                }
                break;
            }
        }
    }

    /**
     * Minimize app, start timer service with the current game&category info only if inactive
     */
    private void startTimerService() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        startActivity(homeIntent);

        if (TimerService.IS_ACTIVE) {
            stopService(new Intent(this, TimerService.class));
        }
        Intent serviceIntent = new Intent(this, TimerService.class);
        serviceIntent.putExtra(getString(R.string.extra_game), gson.toJson(currentGame));
        serviceIntent.putExtra(getString(R.string.extra_category), currentCategory.name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        TimerService.IS_ACTIVE = true;
    }

    /**
     * Listen for timer events:
     * - User clicked on "Close Timer" from the notification
     * - User clicked on "Save & Reset" in the Timer press-and-hold dialog
     */
    private void setupReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                if (action.equals(getString(R.string.action_close_timer))) {
                    if (Chronometer.started) {
                        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                        Dialogs.timerActiveDialog(context).show();
                    } else {
                        stopService(new Intent(context, TimerService.class));
                    }
                } else if (action.equals(getString(R.string.action_save_best_time))) {
                    long time = intent.getLongExtra(getString(R.string.extra_best_time), 0);
                    updateCategory(currentCategory, time);
                } else if (action.equals(getString(R.string.action_save_timer_position))) {
                    int x = intent.getIntExtra(getString(R.string.extra_timer_x), 0);
                    int y = intent.getIntExtra(getString(R.string.extra_timer_y), 0);
                    currentGame.setTimerPosition(x, y);
                } else if (action.equals(getString(R.string.action_increment_run_count))) {
                    updateCategory(currentCategory, currentCategory.runCount + 1);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.action_close_timer));
        intentFilter.addAction(getString(R.string.action_save_best_time));
        intentFilter.addAction(getString(R.string.action_save_timer_position));
        intentFilter.addAction(getString(R.string.action_increment_run_count));
        registerReceiver(receiver, intentFilter);
    }

    private void setupFragments() {
        fragmentManager = getSupportFragmentManager();
        GamesListFragment gamesListFragment =
                (GamesListFragment) fragmentManager.findFragmentByTag(TAG_GAMES_LIST_FRAGMENT);
        GameCategoriesListFragment categoriesListFragment =
                (GameCategoriesListFragment) fragmentManager.findFragmentByTag(TAG_CATEGORY_LIST_FRAGMENT);
        if (categoriesListFragment != null) {
            currentFragment = categoriesListFragment;
            currentFragmentTag = TAG_CATEGORY_LIST_FRAGMENT;
            setActionBarTitleAndUpButton(currentGame.name, true);
        } else if (gamesListFragment != null) {
            currentFragment = gamesListFragment;
            currentFragmentTag = TAG_GAMES_LIST_FRAGMENT;
        } else {
            currentFragment = GamesListFragment.newInstance(games);
            currentFragmentTag = TAG_GAMES_LIST_FRAGMENT;
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, currentFragment, TAG_GAMES_LIST_FRAGMENT)
                    .commit();
        }
    }

    private void setupRateSnackbar() {
        rateSnackbar = Snackbar.make(fabAdd, "If you like this app, please rate it in the Play Store.",
                Snackbar.LENGTH_LONG);
        rateSnackbar.setAction(R.string.rate, view -> {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + getPackageName()));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                    | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT : Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
                    | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                startActivity(marketIntent);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
            }
        });
    }

    private void saveData() {
        sharedPrefs.edit()
                .putString(getString(R.string.key_games), games.isEmpty() ? "" : gson.toJson(games))
                .putInt(getString(R.string.key_launch_counter), launchCounter)
                .putBoolean(getString(R.string.key_rate_snackbar_shown), rateSnackbarShown)
                .apply();
    }

    private void setActionBarTitleAndUpButton(String title, boolean enableUpButton) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
            actionBar.setDisplayHomeAsUpEnabled(enableUpButton);
        }
    }

    private void listFragmentClick() {
        if (currentFragmentTag.equals(TAG_GAMES_LIST_FRAGMENT)) {
            currentGame = getGame(currentFragment.getClickedItemName());
            currentFragment = GameCategoriesListFragment.newInstance(currentGame);
            currentFragmentTag = TAG_CATEGORY_LIST_FRAGMENT;
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.fragment_transition_in, R.anim.fragment_transition_out,
                            R.anim.fragment_transition_pop_in, R.anim.fragment_transition_pop_out)
                    .replace(R.id.fragment_container, currentFragment, TAG_CATEGORY_LIST_FRAGMENT)
                    .addToBackStack(null)
                    .commit();
            setActionBarTitleAndUpButton(currentGame.name, true);
        } else if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            currentCategory = currentGame.getCategory(currentFragment.getClickedItemName());
            checkPermissionAndStartTimer();
        }
    }

    private void listFragmentEdit() {
        String selectedItemName = currentFragment.getSelectedItemNames()[0];
        if (currentFragmentTag.equals(TAG_GAMES_LIST_FRAGMENT)) {
            Dialogs.editGameDialog(this, getGame(selectedItemName)).show();
        } else if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            Dialogs.editCategoryDialog(this, currentGame.getCategory(selectedItemName)).show();
        }
    }

    private void listFragmentDelete() {
        String[] selectedItems = currentFragment.getSelectedItemNames();
        if (currentFragmentTag.equals(TAG_GAMES_LIST_FRAGMENT)) {
            Game[] selectedGames = new Game[selectedItems.length];
            for (int i = 0; i < selectedGames.length; i++) {
                selectedGames[i] = getGame(selectedItems[i]);
            }
            actionDeleteGames(selectedGames);
        } else if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            Category[] selectedCategories = new Category[selectedItems.length];
            for (int i = 0; i < selectedCategories.length; i++) {
                selectedCategories[i] = currentGame.getCategory(selectedItems[i]);
            }
            actionDeleteCategories(selectedCategories);
        }
    }

    public void addFabButtonPressed(View view) {
        addSnackbar.dismiss();
        if (currentFragmentTag.equals(TAG_GAMES_LIST_FRAGMENT)) {
            Dialogs.newGameDialog(this).show();
        } else if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            Dialogs.newCategoryDialog(this).show();
        }
        currentFragment.finishActionMode();
    }

    private void actionDeleteCategories(final Category[] toRemove) {
        if (toRemove.length == 1) {
            Category category = toRemove[0];
            long bestTime = category.bestTime;
            if (bestTime > 0) {
                Dialogs.deleteCategoryDialog(this, toRemove).show();
            } else {
                removeCategories(toRemove);
            }
        } else {
            Dialogs.deleteCategoriesDialog(this, toRemove).show();
        }
    }

    void actionDeleteGames(final Game[] toRemove) {
        Dialogs.deleteGamesDialog(this, toRemove).show();
    }

    void addGame(String gameName) {
        games.add(new Game(gameName));
        ((GamesListFragment)currentFragment).updateData(games);
    }

    void editGameName(Game game, String newName) {
        game.name = newName;
        ((GamesListFragment) currentFragment).updateData(games);
    }

    void removeGames(Game[] toRemove) {
        games.removeAll(Arrays.asList(toRemove));
        ((GamesListFragment) currentFragment).updateData(games);
    }

    void addCategory(String category) {
        currentGame.addCategory(category);
        ((GameCategoriesListFragment) currentFragment).updateData(currentGame);
    }

    void removeCategories(Category[] toRemove) {
        currentGame.categories.removeAll(Arrays.asList(toRemove));
        ((GameCategoriesListFragment) currentFragment).updateData(currentGame);
    }

    void updateCategory(Category category, long time, int runCount) {
        category.bestTime = time;
        category.runCount = runCount;
        if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            ((GameCategoriesListFragment) currentFragment).updateData(currentGame);
        }
        saveData();
    }

    void updateCategory(Category category, long time) {
        updateCategory(category, time, category.runCount);
    }

    void updateCategory(Category category, int runCount) {
        updateCategory(category, category.bestTime, runCount);
    }

    void editCategory(Category category, long newBestTime, int newRunCount) {
        long prevBestTime = category.bestTime;
        int prevRunCount = category.runCount;
        updateCategory(category, newBestTime, newRunCount);
        showEditedCategorySnackbar(category, prevBestTime, prevRunCount);
    }

    private void showEditedCategorySnackbar(Category category, long prevBestTime, int prevRunCount) {
        String message = String.format("%s %s has been edited.", currentGame.name, category.name);
        Snackbar.make(fabAdd, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, view -> updateCategory(category, prevBestTime, prevRunCount))
                .show();
    }

    private Game getGame(String gameName) {
        return games.get(games.indexOf(new Game(gameName)));
    }
}
