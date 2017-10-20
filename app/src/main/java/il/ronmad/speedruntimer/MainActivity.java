package il.ronmad.speedruntimer;

import android.app.Dialog;
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

    private static boolean backFromPermissionCheck = false;

    private FragmentManager fragmentManager;
    private BroadcastReceiver receiver;
    private SharedPreferences sharedPrefs;

    private List<Game> games;
    private Game currentGame;
    private String currentCategory;

    private Toolbar toolbar;
    private FloatingActionButton fabAdd;
    private Snackbar mSnackbar;

    private static final String TAG_NEW_GAME_DIALOG = "NewGameDialog";
    private static final String TAG_EDIT_GAME_DIALOG = "EditGameDialog";
    private static final String TAG_NEW_CATEGORY_DIALOG = "NewCategoryDialog";
    private static final String TAG_EDIT_PB_DIALOG = "EditPBDialog";
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

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fabAdd = (FloatingActionButton) findViewById(R.id.fabAdd);
        mSnackbar = Snackbar.make(fabAdd, R.string.fab_add_str, Snackbar.LENGTH_LONG);

        sharedPrefs = getPreferences(MODE_PRIVATE);
        String savedData = sharedPrefs.getString(getString(R.string.games), "");
        if (savedData.isEmpty()) {
            games = new ArrayList<>();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSnackbar.show();
                }
            }, 1000);
        } else {
            Game[] gameArr = gson.fromJson(savedData, Game[].class);
            games = new ArrayList<>(Arrays.asList(gameArr));
        }
        currentGame = null;
        currentCategory = null;

        setupReceiver();

        fragmentManager = getSupportFragmentManager();
        GameCategoriesListFragment categoriesListFragment =
                (GameCategoriesListFragment) fragmentManager.findFragmentByTag(TAG_CATEGORY_LIST_FRAGMENT);
        if (categoriesListFragment != null) {
            currentGame = games.get(games.indexOf(categoriesListFragment.getGame()));
            categoriesListFragment.resetData(currentGame);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(currentGame.getName());
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, GamesListFragment.newInstance(games), TAG_GAMES_LIST_FRAGMENT)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (backFromPermissionCheck) {
            backFromPermissionCheck = false;
            if (Build.VERSION.SDK_INT >= 23 && !TimerService.IS_ACTIVE) {
                if (Settings.canDrawOverlays(this)) {
                    startTimerService();
                } else {
                    checkOverlayPermissionDelayed();
                }
            }
        } else if (TimerService.IS_ACTIVE) {
            showCloseTimerOnResumeDialog();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveGameData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, TimerService.class));
        unregisterReceiver(receiver);
    }

    @Override
    public void onGamesListFragmentInteraction(ListAction action, int[] positions) {
        switch (action) {
            case CLICK:
                currentGame = games.get(positions[0]);
                fragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fragment_transition_in, R.anim.fragment_transition_out,
                                R.anim.fragment_transition_pop_in, R.anim.fragment_transition_pop_out)
                        .replace(R.id.fragment_container,
                                GameCategoriesListFragment.newInstance(currentGame),
                                TAG_CATEGORY_LIST_FRAGMENT)
                        .addToBackStack(null)
                        .commit();
                toolbar.setTitle(currentGame.getName());
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setDisplayHomeAsUpEnabled(true);
                }
                break;
            case EDIT:
                currentGame = games.get(positions[0]);
                actionEditGameName(positions[0]);
                break;
            case DELETE:
                actionDeleteGames(positions);
                break;
            default:

                break;
        }

    }

    @Override
    public void onCategoryListFragmentInteraction(ListAction action, String[] categories) {
        switch (action) {
            case CLICK:
                currentCategory = categories[0];
                checkDrawOverlayPermission();
                break;
            case EDIT:
                currentCategory = categories[0];
                new MyDialog().show(fragmentManager, TAG_EDIT_PB_DIALOG);
                break;
            case DELETE:
                actionDeleteCategories(categories);
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
                fragmentManager.popBackStack();
                toolbar.setTitle(getString(R.string.app_name));
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        GamesListFragment gamesListFragment = (GamesListFragment)fragmentManager.findFragmentByTag(TAG_GAMES_LIST_FRAGMENT);
        if (gamesListFragment != null && gamesListFragment.isVisible()) {
            super.onBackPressed();
        } else {
            backToGamesListFragment();
        }
    }

    private void backToGamesListFragment() {
        fragmentManager.popBackStack();
        toolbar.setTitle(getString(R.string.app_name));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    final static int OVERLAY_REQUEST_CODE = 251;
    private void checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
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

    // All of this is because the permission may take time to register.
    private void checkOverlayPermissionDelayed() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 23 && Settings.canDrawOverlays(MainActivity.this)) {
                    startTimerService();
                } else {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (Build.VERSION.SDK_INT >= 23 && Settings.canDrawOverlays(MainActivity.this)) {
                                startTimerService();
                            } else {
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (Build.VERSION.SDK_INT >= 23 && Settings.canDrawOverlays(MainActivity.this)) {
                                            startTimerService();
                                        }
                                    }
                                }, 500);
                            }
                        }
                    }, 500);
                }
            }
        }, 500);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case OVERLAY_REQUEST_CODE: {
                if (Build.VERSION.SDK_INT >= 23) {
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

    private void startTimerService() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        startActivity(homeIntent);

        if (TimerService.IS_ACTIVE) {
            stopService(new Intent(this, TimerService.class));
        }
        Intent serviceIntent = new Intent(this, TimerService.class);
        serviceIntent.putExtra(getString(R.string.game), gson.toJson(currentGame));
        serviceIntent.putExtra(getString(R.string.category_name), currentCategory);
        startService(serviceIntent);
        TimerService.IS_ACTIVE = true;
    }

    private void setupReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(getString(R.string.action_close_timer))) {
                    if (Chronometer.started) {
                        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                        AlertDialog dialog = new AlertDialog.Builder(context)
                                .setMessage("Timer is active. Close anyway?")
                                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        stopService(new Intent(MainActivity.this, TimerService.class));
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, null)
                                .create();
                        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        dialog.show();
                    } else {
                        stopService(new Intent(context, TimerService.class));
                    }
                } else if (action.equals(getString(R.string.action_save_best_time))) {
                    long time = intent.getLongExtra(getString(R.string.best_time), 0);
                    updateBestTime(time);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.action_close_timer));
        intentFilter.addAction(getString(R.string.action_save_best_time));
        registerReceiver(receiver, intentFilter);
    }

    private void saveGameData() {
        sharedPrefs.edit()
                .putString(getString(R.string.games), games.isEmpty() ? "" : gson.toJson(games))
                .apply();
    }

    private void showCloseTimerOnResumeDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Timer must be closed in order to use the app.")
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        stopService(new Intent(MainActivity.this, TimerService.class));
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                        homeIntent.addCategory(Intent.CATEGORY_HOME);
                        startActivity(homeIntent);
                    }
                })
                .create()
                .show();
    }

    public void addFabButtonPressed(View view) {
        mSnackbar.dismiss();
        GamesListFragment gamesListFragment = (GamesListFragment)fragmentManager.findFragmentByTag(TAG_GAMES_LIST_FRAGMENT);
        if (gamesListFragment != null && gamesListFragment.isVisible()) {
            new MyDialog().show(fragmentManager, TAG_NEW_GAME_DIALOG);
        } else {
            new MyDialog().show(fragmentManager, TAG_NEW_CATEGORY_DIALOG);
        }
    }

    public void actionEditGameName(int position) {
        MyDialog dialog = new MyDialog();
        Bundle args = new Bundle();
        args.putInt("position", position);
        dialog.setArguments(args);
        dialog.show(fragmentManager, TAG_EDIT_GAME_DIALOG);
}

    private void actionDeleteCategories(final String[] categories) {
        if (categories.length == 1) {
            long bestTime = currentGame.getBestTime(categories[0]);
            if (bestTime > 0) {
                new AlertDialog.Builder(this)
                        .setTitle(String.format("Delete %s %s?", currentGame.getName(), categories[0]))
                        .setMessage(String.format("Your PB of %s will be lost.", Game.getFormattedBestTime(bestTime)))
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                removeCategories(categories);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create().show();
            } else {
                removeCategories(categories);
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Delete selected categories?")
                    .setMessage("Your PBs will be lost.")
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            removeCategories(categories);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
        }
    }

    private void actionDeleteGames(final int[] positions) {
        new AlertDialog.Builder(this)
                .setTitle("Delete selected games?")
                .setMessage("All categories and PBs associated with the games will be lost.")
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        removeGames(positions);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    private void addGame(String gameName) {
        games.add(new Game(gameName));
        GamesListFragment gamesListFragment = (GamesListFragment)fragmentManager.findFragmentByTag(TAG_GAMES_LIST_FRAGMENT);
        if (gamesListFragment != null) {
            gamesListFragment.addGame(gameName);
        }
    }

    private void addCategory(String category) {
        currentGame.addCategory(category);
        GameCategoriesListFragment categoryListFragment =
                (GameCategoriesListFragment)fragmentManager.findFragmentByTag(TAG_CATEGORY_LIST_FRAGMENT);
        if (categoryListFragment != null) {
            categoryListFragment.addCategory(category);
        }
    }

    private void editGameName(int position, String newName) {
        games.get(position).setName(newName);
        GamesListFragment gamesListFragment = (GamesListFragment)fragmentManager.findFragmentByTag(TAG_GAMES_LIST_FRAGMENT);
        if (gamesListFragment != null) {
            gamesListFragment.setGameName(position, newName);
        }
    }

    private void removeGames(int[] positions) {
        Game[] toRemove = new Game[positions.length];
        for (int i = 0; i < positions.length; i++) {
            toRemove[i] = games.get(positions[i]);
        }
        games.removeAll(Arrays.asList(toRemove));
        GamesListFragment gamesListFragment = (GamesListFragment)fragmentManager.findFragmentByTag(TAG_GAMES_LIST_FRAGMENT);
        if (gamesListFragment != null) {
            gamesListFragment.removeGames(positions);
        }
    }

    private void removeCategories(String[] categories) {
        for (String category : categories) {
            currentGame.removeCategory(category);
        }
        GameCategoriesListFragment categoryListFragment =
                (GameCategoriesListFragment)fragmentManager.findFragmentByTag(TAG_CATEGORY_LIST_FRAGMENT);
        if (categoryListFragment != null) {
            categoryListFragment.removeCategories(categories);
        }
    }

    void updateBestTime(long time) {
        currentGame.setBestTime(currentCategory, time);
        GameCategoriesListFragment categoryListFragment =
                (GameCategoriesListFragment)fragmentManager.findFragmentByTag(TAG_CATEGORY_LIST_FRAGMENT);
        if (categoryListFragment != null) {
            categoryListFragment.setBestTime(currentCategory, time);
        }
        sharedPrefs.edit()
                .putString(getString(R.string.games), gson.toJson(games))
                .apply();
    }

    public static class MyDialog extends DialogFragment {
        private MainActivity activity;
        private LayoutInflater inflater;
        private EditText newGameInput;
        private MyAutoCompleteTextView newCategoryInput;
        private EditText hoursInput;
        private EditText minutesInput;
        private EditText secondsInput;
        private EditText millisInput;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            activity = (MainActivity) getActivity();
            inflater = activity.getLayoutInflater();
            if (getTag().equals(TAG_NEW_GAME_DIALOG)) {
                return createNewGameDialog();
            }
            if (getTag().equals(TAG_EDIT_GAME_DIALOG)) {
                if (getArguments() != null) {
                    return createEditGameDialog(getArguments().getInt("position"));
                }
            }
            if (getTag().equals(TAG_NEW_CATEGORY_DIALOG)) {
                return createNewCategoryDialog();
            }
            if (getTag().equals(TAG_EDIT_PB_DIALOG)) {
                return createEditPBDialog();
            }
            return null;
        }

        private AlertDialog createNewGameDialog() {
            View dialogView = inflater.inflate(R.layout.new_game_dialog, null);
            newGameInput = (EditText) dialogView.findViewById(R.id.newGameNameInput);
            final AlertDialog dialog =  new AlertDialog.Builder(activity)
                    .setTitle("New game")
                    .setView(dialogView)
                    .setPositiveButton(R.string.create, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Button createButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    createButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String newGameName = newGameInput.getText().toString();
                            if (newGameName.isEmpty()) {
                                newGameInput.setError(getString(R.string.error_empty_game));
                                newGameInput.requestFocus();
                            } else if (activity.games.contains(new Game(newGameName))) {
                                newGameInput.setError(getString(R.string.error_game_already_exists));
                                newGameInput.requestFocus();
                            } else {
                                activity.addGame(newGameName);
                                dialog.dismiss();
                            }
                        }
                    });
                }
            });
            return dialog;
        }

        private AlertDialog createEditGameDialog(final int position) {
            View dialogView = inflater.inflate(R.layout.new_game_dialog, null);
            newGameInput = (EditText) dialogView.findViewById(R.id.newGameNameInput);
            final AlertDialog dialog =  new AlertDialog.Builder(activity)
                    .setTitle("Edit name")
                    .setView(dialogView)
                    .setPositiveButton(R.string.save, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Button createButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    createButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String newGameName = newGameInput.getText().toString();
                            if (newGameName.isEmpty()) {
                                newGameInput.setError(getString(R.string.error_empty_game));
                                newGameInput.requestFocus();
                            } else if (activity.games.contains(new Game(newGameName))) {
                                newGameInput.setError(getString(R.string.error_game_already_exists));
                                newGameInput.requestFocus();
                            } else {
                                activity.editGameName(position, newGameName);
                                dialog.dismiss();
                            }
                        }
                    });
                }
            });
            return dialog;
        }

        private AlertDialog createNewCategoryDialog() {
            View dialogView = inflater.inflate(R.layout.new_category_dialog, null);
            newCategoryInput = (MyAutoCompleteTextView) dialogView.findViewById(R.id.newCategoryInput);
            final AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle("New category")
                    .setView(dialogView)
                    .setPositiveButton(R.string.create, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Button createButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                    createButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String newCategory = newCategoryInput.getText().toString();
                            if (newCategory.isEmpty()) {
                                newCategoryInput.setError(getString(R.string.error_empty_category));
                                newCategoryInput.requestFocus();
                            } else if (activity.currentGame.hasCategory(newCategory)) {
                                newCategoryInput.setError(getString(R.string.error_category_already_exists));
                                newCategoryInput.requestFocus();
                            } else {
                                activity.addCategory(newCategory);
                                dialog.dismiss();
                            }
                        }
                    });
                }
            });
            return dialog;
        }
        private AlertDialog createEditPBDialog() {
            LayoutInflater inflater = activity.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.edit_pb_dialog, null);
            hoursInput = (EditText) dialogView.findViewById(R.id.hours);
            minutesInput = (EditText) dialogView.findViewById(R.id.minutes);
            secondsInput = (EditText) dialogView.findViewById(R.id.seconds);
            millisInput = (EditText) dialogView.findViewById(R.id.milliseconds);
            final long bestTime = activity.currentGame.getBestTime(activity.currentCategory);
            if (bestTime > 0) {
                setTextsFromBestTime(bestTime);
            }
            final AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle(String.format("Edit best time for %s %s", activity.currentGame.getName(), activity.currentCategory))
                    .setView(dialogView)
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            long newTime = getTimeFromEditTexts();
                            activity.updateBestTime(newTime);
                            showEditedPBSnackbar(bestTime, newTime == 0);
                        }
                    })
                    .setNegativeButton(R.string.pb_clear, null)
                    .setNeutralButton(android.R.string.cancel, null)
                    .create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Button clearButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                    clearButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            hoursInput.setText("");
                            minutesInput.setText("");
                            secondsInput.setText("");
                            millisInput.setText("");
                        }
                    });
                }
            });
            return dialog;
        }

        private void showEditedPBSnackbar(final long prevBestTime, boolean cleared) {
            String message = String.format("Best time for %s %s has been %s.", activity.currentGame.getName(),
                    activity.currentCategory, cleared ? "reset" : "edited");
            Snackbar.make(activity.fabAdd, message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            activity.updateBestTime(prevBestTime);
                        }
                    })
                    .show();
        }

        private void setTextsFromBestTime(long bestTime) {
            int hours = (int)(bestTime / (3600 * 1000));
            int remaining = (int)(bestTime % (3600 * 1000));
            int minutes = remaining / (60 * 1000);
            remaining = remaining % (60 * 1000);
            int seconds = remaining / 1000;
            int milliseconds = remaining % 1000;
            hoursInput.setText(hours > 0 ? ""+hours : "");
            minutesInput.setText(minutes > 0 ? ""+minutes : "");
            secondsInput.setText(seconds > 0 ? ""+seconds : "");
            millisInput.setText(milliseconds > 0 ? ""+milliseconds : "");
        }

        private long getTimeFromEditTexts() {
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
