package il.ronmad.speedruntimer;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.exceptions.RealmException;

public class MainActivity extends AppCompatActivity implements BaseListFragment.OnListFragmentInteractionListener {

    private static int launchCounter = 0;
    private static boolean backFromPermissionCheck = false;

    private FragmentManager fragmentManager;
    private BaseListFragment currentFragment;
    private String currentFragmentTag = null;

    private BroadcastReceiver receiver;
    private SharedPreferences sharedPrefs;
    private Realm realm;
    private RealmChangeListener<Realm> realmChangeListener;

    FloatingActionButton fabAdd;
    private boolean rateSnackbarShown;
    private boolean addGamesSnackbarShown;

    Game currentGame;
    Category currentCategory;
    PackageManager packageManager;
    List<ApplicationInfo> installedApps;
    String[] installedGames;

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

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        setupRealm();

        if (savedInstanceState != null) {
            String currentGameName = savedInstanceState.getString("currentGame");
            if (currentGameName != null) {
                currentGame = realm.where(Game.class).equalTo("name", currentGameName).findFirst();
            } else {
                currentGame = null;
            }
        } else {
            currentGame = null;
        }
        currentCategory = null;

        setupInstalledAppsLists();
        setupSnackbars();
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
        sharedPrefs.edit()
                .putInt(getString(R.string.key_launch_counter), launchCounter)
                .putBoolean(getString(R.string.key_rate_snackbar_shown), rateSnackbarShown)
                .putBoolean(getString(R.string.key_add_games_snackbar_shown), addGamesSnackbarShown)
                .apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, TimerService.class));
        unregisterReceiver(receiver);
        realm.removeChangeListener(realmChangeListener);
        realm.close();
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            menu.findItem(R.id.menu_add_games).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_add_games:
                addInstalledGames();
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
        currentGame = null;
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
     * Start game or minimize app, start timer service with the current game&category info only if inactive
     */
    private void startTimerService() {
        if (!tryLaunchGame()) {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            startActivity(homeIntent);
        }
        if (TimerService.IS_ACTIVE) {
            stopService(new Intent(this, TimerService.class));
        }
        Intent serviceIntent = new Intent(this, TimerService.class);
        serviceIntent.putExtra(getString(R.string.extra_game), currentGame.name);
        serviceIntent.putExtra(getString(R.string.extra_category), currentCategory.name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        TimerService.IS_ACTIVE = true;
    }

    private void setupRealm() {
        String savedData = sharedPrefs.getString(getString(R.string.key_games), "");
        if (savedData.isEmpty()) {
            realm = Realm.getDefaultInstance();
        } else {
            Realm.deleteRealm(Realm.getDefaultConfiguration());
            realm = Realm.getDefaultInstance();
            try {
                realm.executeTransaction(realm ->
                        realm.createAllFromJson(Game.class, savedData));
            } catch (RealmException e) {
                realm.executeTransaction(realm ->
                        realm.createAllFromJson(Game.class, Util.migrateJson(savedData)));
            }
            sharedPrefs.edit()
                    .remove(getString(R.string.key_games))
                    .apply();
        }
        realmChangeListener = realm -> currentFragment.update();
        realm.addChangeListener(realmChangeListener);
    }

    private void setupSnackbars() {
        boolean toShowRateSnackbar = false;
        boolean toShowAddGamesSnackbar;
        rateSnackbarShown = sharedPrefs.getBoolean(getString(R.string.key_rate_snackbar_shown), false);
        if (!rateSnackbarShown && launchCounter == 0 && !realm.isEmpty()) {
            int savedLaunchCounter = sharedPrefs.getInt(getString(R.string.key_launch_counter), 0);
            launchCounter = Math.min(3, savedLaunchCounter) + 1;
            toShowRateSnackbar = launchCounter == 3;
        }

        addGamesSnackbarShown = sharedPrefs.getBoolean(getString(R.string.key_add_games_snackbar_shown), false);
        toShowAddGamesSnackbar = countAvailableInstalledGames() > 0 && !addGamesSnackbarShown;

        if (toShowAddGamesSnackbar) {
            new Handler().postDelayed(this::showAddInstalledGamesSnackbar, 1000);
            addGamesSnackbarShown = true;
        } else if (toShowRateSnackbar) {
            new Handler().postDelayed(this::showRateSnackbar, 1000);
            rateSnackbarShown = true;
        }
    }

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
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.action_close_timer));
        registerReceiver(receiver, intentFilter);
    }

    private void setupFragments() {
        fragmentManager = getSupportFragmentManager();
        GamesListFragment gamesListFragment =
                (GamesListFragment) fragmentManager.findFragmentByTag(TAG_GAMES_LIST_FRAGMENT);
        CategoryListFragment categoriesListFragment =
                (CategoryListFragment) fragmentManager.findFragmentByTag(TAG_CATEGORY_LIST_FRAGMENT);
        if (categoriesListFragment != null) {
            currentFragment = categoriesListFragment;
            currentFragmentTag = TAG_CATEGORY_LIST_FRAGMENT;
            setActionBarTitleAndUpButton(currentGame.name, true);
        } else if (gamesListFragment != null) {
            currentFragment = gamesListFragment;
            currentFragmentTag = TAG_GAMES_LIST_FRAGMENT;
        } else {
            currentFragment = GamesListFragment.newInstance();
            currentFragmentTag = TAG_GAMES_LIST_FRAGMENT;
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, currentFragment, TAG_GAMES_LIST_FRAGMENT)
                    .commit();
        }
    }

    private void setupInstalledAppsLists() {
        packageManager = getPackageManager();
        List<ApplicationInfo> allInstalledApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        installedApps = new ArrayList<>();
        List<String> installedGamesList = new ArrayList<>();
        for (ApplicationInfo packageInfo : allInstalledApps) {
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    || packageInfo.packageName.equals(getPackageName())) continue;
            installedApps.add(packageInfo);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && packageInfo.category == ApplicationInfo.CATEGORY_GAME) {
                installedGamesList.add(packageManager.getApplicationLabel(packageInfo).toString());
            }
        }
        installedGames = installedGamesList.toArray(new String[]{});
    }

    private void showRateSnackbar() {
        Snackbar snackbar = Snackbar.make(fabAdd, getString(R.string.rate_snackbar),
                Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.rate, view -> {
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
        snackbar.show();
    }

    private void showAddInstalledGamesSnackbar() {
        Snackbar snackbar = Snackbar.make(fabAdd, getString(R.string.add_games_snackbar), Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.add, view -> addInstalledGames());
        snackbar.show();
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
            currentGame = (Game) currentFragment.getClickedItem();
            currentFragment = CategoryListFragment.newInstance(currentGame.name);
            currentFragmentTag = TAG_CATEGORY_LIST_FRAGMENT;
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.fragment_transition_in, R.anim.fragment_transition_out,
                            R.anim.fragment_transition_pop_in, R.anim.fragment_transition_pop_out)
                    .replace(R.id.fragment_container, currentFragment, TAG_CATEGORY_LIST_FRAGMENT)
                    .addToBackStack(null)
                    .commit();
            setActionBarTitleAndUpButton(currentGame.name, true);
        } else if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            currentCategory = (Category) currentFragment.getClickedItem();
            checkPermissionAndStartTimer();
        }
    }

    private void listFragmentEdit() {
        Object selectedItem = currentFragment.getSelectedItems()[0];
        if (currentFragmentTag.equals(TAG_GAMES_LIST_FRAGMENT)) {
            Dialogs.editGameDialog(this, (Game) selectedItem).show();
        } else if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            Dialogs.editCategoryDialog(this, (Category) selectedItem).show();
        }
    }

    private void listFragmentDelete() {
        Object[] selectedItems = currentFragment.getSelectedItems();
        if (currentFragmentTag.equals(TAG_GAMES_LIST_FRAGMENT)) {
            actionDeleteGames(Arrays.copyOf(selectedItems, selectedItems.length, Game[].class));
        } else if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            actionDeleteCategories(Arrays.copyOf(selectedItems, selectedItems.length, Category[].class));
        }
    }

    public void addFabButtonPressed(View view) {
        if (currentFragmentTag.equals(TAG_GAMES_LIST_FRAGMENT)) {
            Dialogs.newGameDialog(this).show();
        } else if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            Dialogs.newCategoryDialog(this).show();
        }
        currentFragment.finishActionMode();
    }

    private int countAvailableInstalledGames() {
        int existsCount = installedGames.length == 0 ? 0
                : (int) realm.where(Game.class).in("name", installedGames).count();
        return installedGames.length - existsCount;
    }

    private void addInstalledGames() {
        String[] gameNames = new String[countAvailableInstalledGames()];
        int idx = 0;
        for (String name : installedGames) {
            if (!gameExists(name)) {
                gameNames[idx++] = name;
            }
        }
        if (gameNames.length == 0) {
            Toast.makeText(this, getString(R.string.no_games_to_add), Toast.LENGTH_SHORT).show();
        } else {
            Dialogs.addInstalledGamesDialog(this, gameNames).show();
        }
    }

    private boolean tryLaunchGame() {
        if (!sharedPrefs.getBoolean(getString(R.string.key_pref_launch_games), true)) {
            return false;
        }
        ApplicationInfo game = null;
        for (ApplicationInfo appInfo : installedApps) {
            if (currentGame.name.equals(packageManager.getApplicationLabel(appInfo).toString())) {
                game = appInfo;
                break;
            }
        }
        if (game != null) {
            Toast.makeText(this, "Launching " + currentGame.name + "...", Toast.LENGTH_SHORT).show();
            startActivity(packageManager.getLaunchIntentForPackage(game.packageName));
        }
        return game != null;
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
        realm.executeTransaction(realm -> {
            Game game = realm.createObject(Game.class);
            game.name = gameName;
        });
    }

    void editGameName(Game game, String newName) {
        game.setName(newName);
    }

    void removeGames(Game[] toRemove) {
        String[] names = new String[toRemove.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = toRemove[i].name;
        }
        realm.executeTransaction(realm ->
                realm.where(Game.class)
                   .in("name", names)
                   .findAll()
                   .deleteAllFromRealm());
    }

    void addCategory(String categoryName) {
        currentGame.addCategory(categoryName);
    }

    void removeCategories(Category[] toRemove) {
        currentGame.removeCategories(toRemove);
    }

    void updateCategory(Category category, long time, int runCount) {
        category.setData(time, runCount);
    }

    void editCategory(Category category, long newBestTime, int newRunCount) {
        long prevBestTime = category.bestTime;
        int prevRunCount = category.runCount;
        updateCategory(category, newBestTime, newRunCount);
        showEditedCategorySnackbar(category, prevBestTime, prevRunCount);
    }

    boolean gameExists(String name) {
        return realm.where(Game.class).equalTo("name", name).count() > 0;
    }

    boolean categoryExists(String name) {
        return currentGame.categoryExists(name);
    }

    private void showEditedCategorySnackbar(Category category, long prevBestTime, int prevRunCount) {
        String message = String.format("%s %s has been edited.", currentGame.name, category.name);
        Snackbar.make(fabAdd, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, view -> updateCategory(category, prevBestTime, prevRunCount))
                .show();
    }
}
