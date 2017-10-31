package il.ronmad.speedruntimer;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static il.ronmad.speedruntimer.Util.gson;

public class MainActivity extends AppCompatActivity implements BaseListFragment.OnListFragmentInteractionListener {

    private static int launchCounter = 0;
    private static boolean backFromPermissionCheck = false;

    private static String currentFragmentTag = null;

    private FragmentManager fragmentManager;
    private BaseListFragment currentFragment;
    private BroadcastReceiver receiver;
    private SharedPreferences sharedPrefs;

    private List<Game> games;
    private Game currentGame;
    private String currentCategory;

    private FloatingActionButton fabAdd;
    private Snackbar addSnackbar;
    private Snackbar rateSnackbar;
    private boolean rateSnackbarShown;

    private static final String TAG_NEW_GAME_DIALOG = "NewGameDialog";
    private static final String TAG_EDIT_GAME_DIALOG = "EditGameDialog";
    private static final String TAG_NEW_CATEGORY_DIALOG = "NewCategoryDialog";
    private static final String TAG_EDIT_PB_DIALOG = "EditPBDialog";
    private static final String TAG_GAMES_LIST_FRAGMENT = "GamesListFragment";
    private static final String TAG_CATEGORY_LIST_FRAGMENT = "CategoryListFragment";
    private static final String ARG_SELECTED_ITEM = "selected-item";

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

        sharedPrefs = getPreferences(MODE_PRIVATE);
        String savedData = sharedPrefs.getString(getString(R.string.key_games), "");
        if (savedData.isEmpty()) {
            games = new ArrayList<>();
            new Handler().postDelayed(() -> addSnackbar.show(), 1000);
        } else {
            Game[] gameArr = gson.fromJson(savedData, Game[].class);
            games = new ArrayList<>(Arrays.asList(gameArr));
        }
        currentGame = null;
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
            showCloseTimerOnResumeDialog();
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
            currentFragmentTag = null;
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
        serviceIntent.putExtra(getString(R.string.extra_category), currentCategory);
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
                        AlertDialog dialog = new AlertDialog.Builder(context)
                                .setMessage("Timer is active. Close anyway?")
                                .setPositiveButton(R.string.close, (DialogInterface, i) ->
                                        stopService(new Intent(MainActivity.this, TimerService.class))
                                )
                                .setNegativeButton(android.R.string.cancel, null)
                                .create();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                        } else {
                            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        }
                        dialog.show();
                    } else {
                        stopService(new Intent(context, TimerService.class));
                    }
                } else if (action.equals(getString(R.string.action_save_best_time))) {
                    long time = intent.getLongExtra(getString(R.string.extra_best_time), 0);
                    updateBestTime(time);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.action_close_timer));
        intentFilter.addAction(getString(R.string.action_save_best_time));
        registerReceiver(receiver, intentFilter);
    }

    private void setupFragments() {
        fragmentManager = getSupportFragmentManager();
        GamesListFragment gamesListFragment =
                (GamesListFragment) fragmentManager.findFragmentByTag(TAG_GAMES_LIST_FRAGMENT);
        GameCategoriesListFragment categoriesListFragment =
                (GameCategoriesListFragment) fragmentManager.findFragmentByTag(TAG_CATEGORY_LIST_FRAGMENT);
        if (currentFragmentTag == null) {
            currentFragment = GamesListFragment.newInstance(games);
            currentFragmentTag = TAG_GAMES_LIST_FRAGMENT;
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, currentFragment, TAG_GAMES_LIST_FRAGMENT)
                    .commit();
        } else if (currentFragmentTag.equals(TAG_GAMES_LIST_FRAGMENT)) {
            currentFragment = gamesListFragment;
        } else if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            currentGame = games.get(games.indexOf(categoriesListFragment.getGame()));
            setActionBarTitleAndUpButton(currentGame.getName(), true);
            currentFragment = categoriesListFragment;
        }
    }

    private void setupRateSnackbar() {
        rateSnackbar = Snackbar.make(fabAdd, "If you like this app, please rate it on the Play Store.",
                Snackbar.LENGTH_LONG);
        rateSnackbar.setAction(R.string.rate, view -> {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + getPackageName()));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                    | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT : Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET )
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
            currentGame = games.get(games.indexOf(new Game(currentFragment.getClickedItemName())));
            currentFragment = GameCategoriesListFragment.newInstance(currentGame);
            currentFragmentTag = TAG_CATEGORY_LIST_FRAGMENT;
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.fragment_transition_in, R.anim.fragment_transition_out,
                            R.anim.fragment_transition_pop_in, R.anim.fragment_transition_pop_out)
                    .replace(R.id.fragment_container, currentFragment, TAG_CATEGORY_LIST_FRAGMENT)
                    .addToBackStack(null)
                    .commit();
            setActionBarTitleAndUpButton(currentGame.getName(), true);
        } else if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            currentCategory = currentFragment.getClickedItemName();
            checkPermissionAndStartTimer();
        }
    }

    private void listFragmentEdit() {
        MyDialog editDialog = new MyDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_ITEM, currentFragment.getSelectedItemNames()[0]);
        editDialog.setArguments(args);
        if (currentFragmentTag.equals(TAG_GAMES_LIST_FRAGMENT)) {
            editDialog.show(fragmentManager, TAG_EDIT_GAME_DIALOG);
        } else if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            editDialog.show(fragmentManager, TAG_EDIT_PB_DIALOG);
        }
    }

    private void listFragmentDelete() {
        String[] selectedItems = currentFragment.getSelectedItemNames();
        if (currentFragmentTag.equals(TAG_GAMES_LIST_FRAGMENT)) {
            actionDeleteGames(selectedItems);
        } else if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            actionDeleteCategories(selectedItems);
        }
    }

    private void showCloseTimerOnResumeDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Timer must be closed in order to use the app.")
                .setPositiveButton(R.string.close, (dialogInterface, i) ->
                        stopService(new Intent(MainActivity.this, TimerService.class))
                )
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    startActivity(homeIntent);
                })
                .show();
    }

    public void addFabButtonPressed(View view) {
        addSnackbar.dismiss();
        if (currentFragmentTag.equals(TAG_GAMES_LIST_FRAGMENT)) {
            new MyDialog().show(fragmentManager, TAG_NEW_GAME_DIALOG);
        } else if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            new MyDialog().show(fragmentManager, TAG_NEW_CATEGORY_DIALOG);
        }
        currentFragment.finishActionMode();
    }

    private void actionDeleteCategories(final String[] toRemove) {
        if (toRemove.length == 1) {
            long bestTime = currentGame.getBestTime(toRemove[0]);
            if (bestTime > 0) {
                new AlertDialog.Builder(this)
                        .setTitle(String.format("Delete %s %s?", currentGame.getName(), toRemove[0]))
                        .setMessage(String.format("Your PB of %s will be lost.", Game.getFormattedBestTime(bestTime)))
                        .setPositiveButton(R.string.delete, (dialogInterface, i) -> removeCategories(toRemove))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                removeCategories(toRemove);
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Delete selected categories?")
                    .setMessage("Your PBs will be lost.")
                    .setPositiveButton(R.string.delete, (dialogInterface, i) -> removeCategories(toRemove))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private void actionDeleteGames(final String[] toRemove) {
        new AlertDialog.Builder(this)
                .setTitle("Delete selected games?")
                .setMessage("All categories and PBs associated with the games will be lost.")
                .setPositiveButton(R.string.delete, (dialogInterface, i) -> removeGames(toRemove))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void addGame(String gameName) {
        games.add(new Game(gameName));
        ((GamesListFragment)currentFragment).addGame(gameName);
    }

    private void addCategory(String category) {
        currentGame.addCategory(category);
        ((GameCategoriesListFragment)currentFragment).updateData(currentGame);
    }

    private void editGameName(String oldName, String newName) {
        Game game = games.get(games.indexOf(new Game(oldName)));
        game.setName(newName);
        ((GamesListFragment)currentFragment).setGameName(oldName, newName);
    }

    private void removeGames(String[] toRemove) {
        for (String gameName : toRemove) {
            games.remove(new Game(gameName));
        }
        ((GamesListFragment)currentFragment).removeGames(toRemove);
    }

    private void removeCategories(String[] categories) {
        for (String category : categories) {
            currentGame.removeCategory(category);
        }
        ((GameCategoriesListFragment)currentFragment).updateData(currentGame);
    }

    private void setBestTime(String category, long time) {
        currentGame.setBestTime(category, time);
        ((GameCategoriesListFragment)currentFragment).updateData(currentGame);
        saveData();
    }

    void updateBestTime(long time) {
        currentGame.setBestTime(currentCategory, time);
        if (currentFragmentTag.equals(TAG_CATEGORY_LIST_FRAGMENT)) {
            ((GameCategoriesListFragment)currentFragment).updateData(currentGame);
        }
        saveData();
    }

    public static class MyDialog extends DialogFragment {
        private MainActivity activity;
        private LayoutInflater inflater;

        private enum activityMethod {
            ADD_GAME,
            EDIT_GAME,
            ADD_CATEGORY
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            activity = (MainActivity) getActivity();
            inflater = activity.getLayoutInflater();
            if (getTag().equals(TAG_NEW_GAME_DIALOG)) {
                return createNewGameDialog();
            }
            if (getTag().equals(TAG_EDIT_GAME_DIALOG)) {
                return createEditGameDialog(getArguments().getString(ARG_SELECTED_ITEM));
            }
            if (getTag().equals(TAG_NEW_CATEGORY_DIALOG)) {
                return createNewCategoryDialog();
            }
            if (getTag().equals(TAG_EDIT_PB_DIALOG)) {
                return createEditPBDialog(getArguments().getString(ARG_SELECTED_ITEM));
            }
            return null;
        }

        private AlertDialog createNewGameDialog() {
            View dialogView = inflater.inflate(R.layout.new_game_dialog, null);
            EditText newGameInput = dialogView.findViewById(R.id.newGameNameInput);
            AlertDialog dialog =  new AlertDialog.Builder(activity)
                    .setTitle("New game")
                    .setView(dialogView)
                    .setPositiveButton(R.string.create, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            setOnShowListener(dialog, newGameInput, activityMethod.ADD_GAME, "");
            return dialog;
        }

        private AlertDialog createEditGameDialog(String oldName) {
            View dialogView = inflater.inflate(R.layout.new_game_dialog, null);
            EditText newGameInput = dialogView.findViewById(R.id.newGameNameInput);
            AlertDialog dialog =  new AlertDialog.Builder(activity)
                    .setTitle("Edit name")
                    .setView(dialogView)
                    .setPositiveButton(R.string.save, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            setOnShowListener(dialog, newGameInput, activityMethod.EDIT_GAME, oldName);
            return dialog;
        }

        private AlertDialog createNewCategoryDialog() {
            View dialogView = inflater.inflate(R.layout.new_category_dialog, null);
            EditText newCategoryInput = (MyAutoCompleteTextView) dialogView.findViewById(R.id.newCategoryInput);
            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle("New category")
                    .setView(dialogView)
                    .setPositiveButton(R.string.create, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            setOnShowListener(dialog, newCategoryInput, activityMethod.ADD_CATEGORY, "");
            return dialog;
        }
        private AlertDialog createEditPBDialog(String category) {
            LayoutInflater inflater = activity.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.edit_pb_dialog, null);
            EditText hoursInput = dialogView.findViewById(R.id.hours);
            EditText minutesInput = dialogView.findViewById(R.id.minutes);
            EditText secondsInput = dialogView.findViewById(R.id.seconds);
            EditText millisInput = dialogView.findViewById(R.id.milliseconds);
            long bestTime = activity.currentGame.getBestTime(category);
            if (bestTime > 0) {
                setTextsFromBestTime(bestTime, hoursInput, minutesInput, secondsInput, millisInput);
            }
            final AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle(String.format("Edit best time for %s %s", activity.currentGame.getName(), category))
                    .setView(dialogView)
                    .setPositiveButton(R.string.save, (dialogInterface, i) -> {
                        long newTime = getTimeFromEditTexts(hoursInput, minutesInput, secondsInput, millisInput);
                        activity.setBestTime(category, newTime);
                        showEditedPBSnackbar(category, bestTime, newTime == 0);
                    })
                    .setNegativeButton(R.string.pb_clear, null)
                    .setNeutralButton(android.R.string.cancel, null)
                    .create();
            dialog.setOnShowListener(dialogInterface -> {
                Button clearButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                clearButton.setOnClickListener(view -> {
                    hoursInput.setText("");
                    minutesInput.setText("");
                    secondsInput.setText("");
                    millisInput.setText("");
                });
            });
            return dialog;
        }

        private void setOnShowListener(AlertDialog dialog,
                                       EditText textInput,
                                       activityMethod method,
                                       String selectedName) {
            dialog.setOnShowListener(dialogInterface -> {
                Button createButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                createButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String newName = textInput.getText().toString();
                        if (checkForErrors(newName)) {
                            textInput.requestFocus();
                        } else {
                            switch (method) {
                                case ADD_GAME:
                                    activity.addGame(newName);
                                    break;
                                case EDIT_GAME:
                                    activity.editGameName(selectedName, newName);
                                    break;
                                case ADD_CATEGORY:
                                    activity.addCategory(newName);
                                    break;
                            }
                            dialog.dismiss();
                        }
                    }

                    private boolean checkForErrors (String newName) {
                        if (newName.isEmpty()) {
                            textInput.setError(getString(
                                    method == activityMethod.ADD_CATEGORY ? R.string.error_empty_category
                                            : R.string.error_empty_game));
                            return  true;
                        }
                        if (method == activityMethod.ADD_CATEGORY && activity.currentGame.hasCategory(newName)) {
                            textInput.setError(getString(R.string.error_category_already_exists));
                            return  true;
                        }
                        if (method != activityMethod.ADD_CATEGORY && activity.games.contains(new Game(newName))) {
                            textInput.setError(getString(R.string.error_game_already_exists));
                            return  true;
                        }
                        return  false;
                    }
                });
            });
        }

        private void showEditedPBSnackbar(String category, long prevBestTime, boolean cleared) {
            String message = String.format("Best time for %s %s has been %s.", activity.currentGame.getName(),
                    category, cleared ? "reset" : "edited");
            Snackbar.make(activity.fabAdd, message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, view -> activity.setBestTime(category, prevBestTime))
                    .show();
        }

        private void setTextsFromBestTime(long bestTime,
                                          EditText hoursInput,
                                          EditText minutesInput,
                                          EditText secondsInput,
                                          EditText millisInput) {
            int[] units = Util.getTimeUnits(bestTime);
            int hours = units[0], minutes = units[1], seconds = units[2], millis = units[3];
            hoursInput.setText(hours > 0 ? ""+hours : "");
            minutesInput.setText(minutes > 0 ? ""+minutes : "");
            secondsInput.setText(seconds > 0 ? ""+seconds : "");
            millisInput.setText(millis > 0 ? ""+millis : "");
        }

        private long getTimeFromEditTexts(EditText hoursInput,
                                          EditText minutesInput,
                                          EditText secondsInput,
                                          EditText millisInput) {
            String hoursStr = hoursInput.getText().toString();
            String minutesStr = minutesInput.getText().toString();
            String secondsStr = secondsInput.getText().toString();
            String millisStr = millisInput.getText().toString();
            int hours = hoursStr.isEmpty() ? 0 : Integer.parseInt(hoursStr);
            int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);
            int seconds = secondsStr.isEmpty() ? 0 : Integer.parseInt(secondsStr);
            int millis = millisStr.isEmpty() ? 0 : Integer.parseInt(millisStr);
            return 1000*60*60 * hours + 1000*60 * minutes + 1000 * seconds + millis;
        }
    }
}
