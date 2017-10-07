package il.ronmad.speedruntimer;

import android.app.Dialog;
import android.app.DialogFragment;
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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static boolean backFromPermissionCheck = false;

    private BroadcastReceiver receiver;
    private SharedPreferences sharedPrefs;
    private Gson gson;

    private List<Game> games;
    private Game currentGame;
    private String currentCategory;

    private Spinner gameNameSpinner;
    private ArrayAdapter<String> gameNameSpinnerAdapter;
    private Spinner categorySpinner;
    private ArrayAdapter<String> categorySpinnerAdapter;

    private Toolbar toolbar;
    private Button letsGoButton;
    private TextView pbText;
    private TextView pbTime;
    private FloatingActionButton fabAdd;
    private Snackbar mSnackbar;

    private AlertDialog closeTimerDialog;

    // defintetly new and improved, not at all ripped off. and withoutt typos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPrefs = getPreferences(MODE_PRIVATE);
        gson = new GsonBuilder().create();

        letsGoButton = (Button) findViewById(R.id.startButton);
        pbText = (TextView) findViewById(R.id.pbText);
        pbTime = (TextView) findViewById(R.id.pbTime);
        fabAdd = (FloatingActionButton) findViewById(R.id.fabAdd);
        mSnackbar = Snackbar.make(fabAdd, R.string.fab_add_str, Snackbar.LENGTH_LONG);

        String savedData = sharedPrefs.getString(getString(R.string.games), "");
        Log.v("savedData", savedData.isEmpty() ? "Nothing" : savedData);
        if (savedData.isEmpty()) {
            games = new ArrayList<>();
            new Handler().postDelayed(() -> mSnackbar.show(), 1000);
        } else {
            Game[] gameArr = gson.fromJson(savedData, Game[].class);
            games = new ArrayList<>(Arrays.asList(gameArr));
        }
        currentGame = null;
        currentCategory = null;

        closeTimerDialog = new AlertDialog.Builder(this)
                .setMessage("Timer is active. Close anyway?")
                .setPositiveButton(R.string.close, (dialogInterface, i) ->
                        stopService(new Intent(this, TimerService.class)))
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        setupSpinners();
        setupReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (backFromPermissionCheck) {
            backFromPermissionCheck = false;
            if (Build.VERSION.SDK_INT >= 23) {
                if (Settings.canDrawOverlays(this)) {
                    startTimerService();
                } else {
                    // All of this is because the permission may take time to register.
                    Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        if (Settings.canDrawOverlays(this)) {
                            startTimerService();
                        } else {
                            handler.postDelayed(() -> {
                                if (Settings.canDrawOverlays(this)) {
                                    startTimerService();
                                } else {
                                    handler.postDelayed(() -> {
                                        if (Settings.canDrawOverlays(this)) {
                                            startTimerService();
                                        }
                                    }, 500);
                                }
                            }, 500);
                        }
                    }, 500);
                }
            }
        } else {
            stopService(new Intent(this, TimerService.class));
        }
        if (pbTime != null) {
            handlePBDisplay();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        sharedPrefs.edit()
                .putString(getString(R.string.games), games.isEmpty() ? "" : gson.toJson(games))
                .putInt(getString(R.string.spinner_pos), Math.max(0, gameNameSpinner.getSelectedItemPosition()))
                .apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, TimerService.class));
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean gamesExist = currentGame != null;
        if (gamesExist) {
            menu.findItem(R.id.action_delete_game).setTitle("Delete " + currentGame.getName());
        }
        return gamesExist;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_cat:
                actionDeleteCategory();
                return true;
            case R.id.action_delete_game:
                actionDeleteGame();
                return true;
            case R.id.action_edit_pb:
                new MyDialog().show(getFragmentManager(), "EditPBDialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
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

        if (TimerService.IS_RUNNING) {
            stopService(new Intent(this, TimerService.class));
        }
        Intent serviceIntent = new Intent(this, TimerService.class);
        serviceIntent.putExtra(getString(R.string.game), gson.toJson(currentGame));
        serviceIntent.putExtra(getString(R.string.category_name), currentCategory);
        startService(serviceIntent);
    }

    private void setupSpinners() {
        gameNameSpinner = (Spinner) findViewById(R.id.gameNameSpinner);
        gameNameSpinnerAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, new ArrayList<>());
        gameNameSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameNameSpinner.setAdapter(gameNameSpinnerAdapter);

        categorySpinner = (Spinner) findViewById(R.id.categorySpinner);
        categorySpinnerAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, new ArrayList<>());
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categorySpinnerAdapter);

        gameNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                spinnerAction(gameNameSpinner);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == adapterView.getCount() - 1 && adapterView.getCount() > 1) {
                    newCategoryItemSelected();
                } else {
                    spinnerAction(categorySpinner);
                    if (currentGame != null) {
                        currentGame.setLastSelectedCategoryPosition(i);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                spinnerAction(categorySpinner);
            }
        });

        refreshSpinners();
    }

    private void setupReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(getString(R.string.action_close_timer))) {
                    if (Chronometer.started) {
                        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                        closeTimerDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        closeTimerDialog.show();
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

    private void refreshSpinners() {
        gameNameSpinnerAdapter.clear();
        for (Game game : games) {
            gameNameSpinnerAdapter.add(game.getName());
        }
        gameNameSpinnerAdapter.notifyDataSetChanged();
        setSpinnerSelection(gameNameSpinner, sharedPrefs.getInt(getString(R.string.spinner_pos), 0));
    }

    private void refreshCategorySpinner() {
        categorySpinnerAdapter.clear();
        if (currentGame != null) {
            categorySpinnerAdapter.add(getString(R.string.new_category));
            for (String category : currentGame.getCategories().keySet()) {
                categorySpinnerAdapter.insert(category, 0);
            }
        }
        categorySpinnerAdapter.notifyDataSetChanged();
        setSpinnerSelection(categorySpinner, currentGame == null ? -1 : currentGame.getLastSelectedCategoryPosition());
    }

    private void spinnerAction(Spinner spinner) {
        if (spinner == gameNameSpinner) {
            currentGame = games.isEmpty() ? null : games.get(gameNameSpinner.getSelectedItemPosition());
            refreshCategorySpinner();
        } else if (spinner == categorySpinner) {
            currentCategory = games.isEmpty() ? null : categorySpinner.getSelectedItem().toString();
            handleUIInteraction();
            handlePBDisplay();
        }
    }

    private void setSpinnerSelection(Spinner spinner, int pos) {
        int prevPos = spinner.getSelectedItemPosition();
        spinner.setSelection(pos);
        if (pos == prevPos || prevPos == -1) {  // onItemSelected wasn't called
            spinnerAction(spinner);
        }
    }

    public void launchTimerButtonPressed(View view) {
        checkDrawOverlayPermission();
    }

    public void addGameButtonPressed(View view) {
        mSnackbar.dismiss();
        new MyDialog().show(getFragmentManager(), "NewGameDialog");
    }

    private void actionDeleteCategory() {
        long bestTime = currentGame.getBestTime(currentCategory);
        if (bestTime > 0) {
            new AlertDialog.Builder(this)
                    .setTitle(String.format("Delete %s %s?", currentGame.getName(), currentCategory))
                    .setMessage(String.format("Your PB of %s will be lost.", Game.getFormattedBestTime(bestTime)))
                    .setPositiveButton(R.string.delete, (dialogInterface, i) -> removeCategory())
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
        } else {
            removeCategory();
        }
    }

    private void actionDeleteGame() {
        new AlertDialog.Builder(this)
                .setTitle(String.format("Delete %s?", currentGame.getName()))
                .setMessage(String.format("All categories and PBs associated with %s will be lost.",
                        currentGame.getName()))
                .setPositiveButton(R.string.delete, (dialogInterface, i) -> removeGame())
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    private void newCategoryItemSelected() {
        categorySpinner.setSelection(categorySpinnerAdapter.getPosition(currentCategory));
        new MyDialog().show(getFragmentManager(), "NewCategoryDialog");
    }

    private void handleUIInteraction() {
        boolean gamesExist = currentGame != null;
        gameNameSpinner.setEnabled(gamesExist);
        categorySpinner.setEnabled(gamesExist);
        letsGoButton.setEnabled(gamesExist);
        if (gamesExist) {
            gameNameSpinnerAdapter.remove(getString(R.string.no_games));

        } else if (gameNameSpinnerAdapter.getPosition(getString(R.string.no_games)) == -1) {
            gameNameSpinnerAdapter.add(getString(R.string.no_games));
        }
        gameNameSpinnerAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    private void handlePBDisplay() {
        if (currentGame == null) {
            pbText.setVisibility(View.GONE);
            pbTime.setVisibility(View.GONE);
        } else {
            long bestTime = currentGame.getBestTime(currentCategory);
            pbTime.setText(bestTime == 0 ? "None yet" : Game.getFormattedBestTime(bestTime));
            pbTime.setTextColor(ContextCompat.getColor(this,
                    bestTime == 0 ? android.R.color.primary_text_light : R.color.colorAccent));
            pbText.setVisibility(View.VISIBLE);
            pbTime.setVisibility(View.VISIBLE);
        }
    }

    private void addGameAndCategory(String gameName, String category) {
        Game newGame = new Game(gameName);
        if (!games.contains(newGame)) {
            newGame.addCategory(category);
            games.add(0, newGame);
            gameNameSpinnerAdapter.insert(gameName, 0);
            gameNameSpinnerAdapter.notifyDataSetChanged();
            setSpinnerSelection(gameNameSpinner, 0);
        } else {
            int gameIndex = games.indexOf(newGame);
            games.get(gameIndex).addCategory(category);
            setSpinnerSelection(gameNameSpinner, gameIndex);
        }
    }

    private void removeGame() {
        games.remove(currentGame);
        gameNameSpinnerAdapter.remove(currentGame.getName());
        gameNameSpinnerAdapter.notifyDataSetChanged();
        setSpinnerSelection(gameNameSpinner, Math.max(0, gameNameSpinner.getSelectedItemPosition() - 1));
    }

    private void addCategory(String category) {
        if (currentGame.addCategory(category) == null) {
            refreshCategorySpinner();
        }
    }

    private void removeCategory() {
        currentGame.removeCategory(currentCategory);
        if (currentGame.isEmpty()) {
            removeGame();
        } else {
            categorySpinnerAdapter.remove(currentCategory);
            categorySpinnerAdapter.notifyDataSetChanged();
            setSpinnerSelection(categorySpinner, Math.max(0, categorySpinner.getSelectedItemPosition() - 1));
        }
    }

    void updateBestTime(long time) {
        currentGame.setBestTime(currentCategory, time);
        handlePBDisplay();
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
            if (getTag().equals("NewGameDialog")) {
                return createNewGameDialog();
            }
            if (getTag().equals("NewCategoryDialog")) {
                return createNewCategoryDialog();
            }
            if (getTag().equals("EditPBDialog")) {
                return createEditPBDialog();
            }
            return null;
        }

        private AlertDialog createNewGameDialog() {
            View dialogView = inflater.inflate(R.layout.new_game_dialog, null);
            newGameInput = (EditText) dialogView.findViewById(R.id.newGameNameInput);
            newCategoryInput = (MyAutoCompleteTextView) dialogView.findViewById(R.id.newGameCategoryInput);
            AlertDialog dialog =  new AlertDialog.Builder(activity)
                    .setTitle("New game")
                    .setView(dialogView)
                    .setPositiveButton(R.string.create, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            dialog.setOnShowListener(dialogInterface -> {
                Button createButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                createButton.setOnClickListener(v -> {
                    String newGameName = newGameInput.getText().toString();
                    String newCategory = newCategoryInput.getText().toString();
                    if (newGameName.isEmpty()) {
                        newGameInput.setError(getString(R.string.error_empty_game));
                        newGameInput.requestFocus();
                    } else {
                        activity.addGameAndCategory(newGameName, newCategory);
                        dialog.dismiss();
                    }
                });
            });
            return dialog;
        }

        private AlertDialog createNewCategoryDialog() {
            View dialogView = inflater.inflate(R.layout.new_category_dialog, null);
            newCategoryInput = (MyAutoCompleteTextView) dialogView.findViewById(R.id.newCategoryInput);
            return new AlertDialog.Builder(activity)
                    .setTitle("New category")
                    .setView(dialogView)
                    .setPositiveButton(R.string.create, (dialogInterface, i) -> {
                        String newCategory = newCategoryInput.getText().toString();
                        if (newCategory.isEmpty() || activity.currentGame.hasCategory(newCategory)) {
                            return;
                        }
                        activity.addCategory(newCategory);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }

        private AlertDialog createEditPBDialog() {
            LayoutInflater inflater = activity.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.edit_pb_dialog, null);
            hoursInput = (EditText) dialogView.findViewById(R.id.hours);
            minutesInput = (EditText) dialogView.findViewById(R.id.minutes);
            secondsInput = (EditText) dialogView.findViewById(R.id.seconds);
            millisInput = (EditText) dialogView.findViewById(R.id.milliseconds);
            long bestTime = activity.currentGame.getBestTime(activity.currentCategory);
            if (bestTime > 0) {
                setTextsFromBestTime(bestTime);
            }
            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle(String.format("Edit best time for %s %s", activity.currentGame.getName(), activity.currentCategory))
                    .setView(dialogView)
                    .setPositiveButton(R.string.apply, (dialogInterface, i) -> {
                        long newTime = getTimeFromEditTexts();
                        activity.updateBestTime(newTime);
                        showEditedPBSnackbar(bestTime, newTime == 0);
                    })
                    .setNegativeButton(R.string.pb_clear, null)
                    .setNeutralButton(android.R.string.cancel, null)
                    .create();
            dialog.setOnShowListener(dialogInterface -> {
                Button clearButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                clearButton.setOnClickListener(v -> {
                    hoursInput.setText("");
                    minutesInput.setText("");
                    secondsInput.setText("");
                    millisInput.setText("");
                });
            });
            return dialog;
        }

        private void showEditedPBSnackbar(long prevBestTime, boolean cleared) {
            String message = String.format("Best time for %s %s has been %s.", activity.currentGame.getName(),
                    activity.currentCategory, cleared ? "reset" : "edited");
            Snackbar.make(activity.fabAdd, message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, view -> activity.updateBestTime(prevBestTime))
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
