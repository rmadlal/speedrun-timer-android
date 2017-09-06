package com.example.ronmad.speedruntimer;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int SNACKBAR_WHAT = 0;

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

    private Button letsGoButton;
    private ImageButton deleteGameButton;
    private TextView pbText;
    private FloatingActionButton fabAdd;
    private Snackbar addSnackbar;
    private SnackbarHandler snackbarHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPrefs = getPreferences(MODE_PRIVATE);
        gson = new GsonBuilder().create();

        letsGoButton = (Button) findViewById(R.id.startButton);
        pbText = (TextView) findViewById(R.id.pbText);
        fabAdd = (FloatingActionButton) findViewById(R.id.fabAdd);
        addSnackbar = Snackbar.make(fabAdd, R.string.fab_add_str, Snackbar.LENGTH_INDEFINITE);
        snackbarHandler = new SnackbarHandler(addSnackbar);

        String savedData = sharedPrefs.getString("games", "");
        Log.v("savedData", savedData);
        if (savedData.isEmpty()) {
            games = new ArrayList<>();
        }
        else {
            Game[] gameArr = gson.fromJson(savedData, Game[].class);
            games = new ArrayList<>(Arrays.asList(gameArr));
        }

        setupDeleteButton();
        setupSpinners();

        receiver = new MyReceiver(this);
        IntentFilter intentFilter = new IntentFilter("action_close_timer");
        intentFilter.addAction("action_save_best_time");
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        stopService(new Intent(this, TimerService.class));
        if (pbText != null) {
            handlePbText();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        sharedPrefs.edit()
                   .putString("games", games.isEmpty() ? "" : gson.toJson(games))
                   .putInt("gameNameSpinnerPos", gameNameSpinner.getSelectedItemPosition())
                   .apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, TimerService.class));
        unregisterReceiver(receiver);
    }

    final static int Overlay_REQUEST_CODE = 251;
    private void checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, Overlay_REQUEST_CODE);
            } else {
                startTimerService();
            }
        } else {
            startTimerService();
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
        serviceIntent.putExtra("com.example.ronmad.speedruntimer.game",
                currentGame);
        serviceIntent.putExtra("com.example.ronmad.speedruntimer.category",
                currentCategory);
        startService(serviceIntent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Overlay_REQUEST_CODE: {
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

    private void setupSpinners() {
        gameNameSpinner = (Spinner) findViewById(R.id.gameNameSpinner);
        gameNameSpinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>());
        gameNameSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameNameSpinner.setAdapter(gameNameSpinnerAdapter);

        categorySpinner = (Spinner) findViewById(R.id.categorySpinner);
        categorySpinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>());
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categorySpinnerAdapter);

        gameNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                gameNameSpinnerAction();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                currentGame = null;
            }
        });
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (categorySpinner.isEnabled() && i == adapterView.getCount() - 1) {
                    newCategoryItemSelected();
                }
                else {
                    categorySpinnerAction();
                    if (currentGame != null) {
                        currentGame.setLastSelectedCategoryPosition(i);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                categorySpinnerAction();
            }
        });

        refreshSpinners();
    }

    private void gameNameSpinnerAction() {
        currentGame = games.isEmpty() ? null : games.get(gameNameSpinner.getSelectedItemPosition());
        refreshCategorySpinner();
    }

    private void categorySpinnerAction() {
        currentCategory = games.isEmpty() ? null : categorySpinner.getSelectedItem().toString();
        handleUIInteraction();
        handlePbText();
    }

    private void refreshSpinners() {
        gameNameSpinnerAdapter.clear();
        for (Game game : games) {
            gameNameSpinnerAdapter.add(game.getName());
        }
        gameNameSpinnerAdapter.notifyDataSetChanged();
        gameNameSpinner.setSelection(sharedPrefs.getInt("gameNameSpinnerPos", 0));
        gameNameSpinnerAction();
    }

    private void refreshCategorySpinner() {
        categorySpinnerAdapter.clear();
        categorySpinnerAdapter.add("New category...");
        if (currentGame != null) {
            currentGame.getCategories().forEach((category, bestTime) -> categorySpinnerAdapter.insert(category, 0));
        }
        categorySpinnerAdapter.notifyDataSetChanged();
        categorySpinner.setSelection(currentGame == null ? 0 : currentGame.getLastSelectedCategoryPosition());
        categorySpinnerAction();
    }

    private void setupDeleteButton() {
        deleteGameButton = (ImageButton) findViewById(R.id.deleteGameButton);
        deleteGameButton.setLongClickable(true);
        deleteGameButton.setOnClickListener(view -> {
            long bestTime = currentGame.getBestTime(currentCategory);
            if (bestTime > 0) {
                new AlertDialog.Builder(this)
                        .setTitle("Delete " + currentGame.getName() + currentCategory)
                        .setMessage("Your PB of " + currentGame.getFormattedBestTime(currentCategory) + " will be deleted. Are you sure?")
                        .setPositiveButton(R.string.delete, (dialogInterface, i) -> deleteCategory())
                        .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {})
                        .create().show();
            }
            else {
                deleteCategory();
            }
        });
        deleteGameButton.setOnLongClickListener(view -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete " + currentGame.getName())
                    .setMessage("All categories and PBs will be lost. Are you sure?")
                    .setPositiveButton(R.string.delete, (dialogInterface, i) -> deleteGame())
                    .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {})
                    .create().show();
            return true;
        });
    }

    public void launchTimerButtonPressed(View view) {
        checkDrawOverlayPermission();
    }

    public void addGameButtonPressed(View view) {
        NewGameDialog dialog = new NewGameDialog();
        dialog.activity = this;
        dialog.show(getFragmentManager(), "NewGameDialog");
    }

    private void newCategoryItemSelected() {
        categorySpinner.setSelection(categorySpinnerAdapter.getPosition(currentCategory));
        NewGameDialog dialog = new NewGameDialog();
        dialog.activity = this;
        dialog.show(getFragmentManager(), "NewCategoryDialog");
    }

    private void handleUIInteraction() {
        gameNameSpinner.setEnabled(!games.isEmpty());
        categorySpinner.setEnabled(!games.isEmpty());
        deleteGameButton.setEnabled(!games.isEmpty());
        letsGoButton.setEnabled(!games.isEmpty());
        if (games.isEmpty()) {
            gameNameSpinnerAdapter.add(getString(R.string.no_games));
            deleteGameButton.setColorFilter(ContextCompat.getColor(this, R.color.colorAccentSecondary));
            snackbarHandler.sendEmptyMessageDelayed(SNACKBAR_WHAT, 1000);
        }
        else {
            gameNameSpinnerAdapter.remove(getString(R.string.no_games));
            deleteGameButton.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
            addSnackbar.dismiss();
        }
        gameNameSpinnerAdapter.notifyDataSetChanged();
    }

    private void handlePbText() {
        if (games.isEmpty()) {
            pbText.setVisibility(View.INVISIBLE);
            return;
        }
        if (currentGame.getBestTime(currentCategory) > 0) {
            pbText.setText("PB: " + currentGame.getFormattedBestTime(currentCategory));
            pbText.setVisibility(View.VISIBLE);
        }
        else {
            pbText.setVisibility(View.INVISIBLE);
        }
    }

    private void addGameAndCategory(String gameName, String category) {
        Game newGame = new Game(gameName);
        if (!games.contains(newGame)) {
            newGame.addCategory(category);
            games.add(0, newGame);
            gameNameSpinnerAdapter.insert(gameName, 0);
            gameNameSpinnerAdapter.notifyDataSetChanged();
            gameNameSpinner.setSelection(0);
        }
        else {
            int gameIndex = games.indexOf(newGame);
            games.get(gameIndex).addCategory(category);
            gameNameSpinner.setSelection(gameIndex);
        }
        gameNameSpinnerAction();
    }

    private void addCategory(String category) {
        currentGame.addCategory(category);
        refreshCategorySpinner();
    }

    private void deleteCategory() {
        currentGame.removeCategory(currentCategory);
        if (currentGame.isEmpty()) {
            deleteGame();
        }
        else {
            categorySpinnerAdapter.remove(currentCategory);
            categorySpinnerAdapter.notifyDataSetChanged();
            categorySpinner.setSelection(Math.max(0, categorySpinner.getSelectedItemPosition() - 1));
            categorySpinnerAction();
        }
    }

    private void deleteGame() {
        games.remove(currentGame);
        gameNameSpinnerAdapter.remove(currentGame.getName());
        gameNameSpinnerAdapter.notifyDataSetChanged();
        gameNameSpinner.setSelection(Math.max(0, gameNameSpinner.getSelectedItemPosition() - 1));
        gameNameSpinnerAction();
    }

    public void updateBestTime(long time) {
        currentGame.setBestTime(currentCategory, time);
        sharedPrefs.edit()
            .putString("games", gson.toJson(games))
            .apply();
    }

    private static class MyReceiver extends BroadcastReceiver {
        private MainActivity activity;

        public MyReceiver(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("action_close_timer")) {
                if (Chronometer.started) {
                    AlertDialog closeDialog = new AlertDialog.Builder(context)
                        .setTitle("Timer is active")
                        .setMessage("Close anyway?")
                        .setPositiveButton(android.R.string.yes, (dialogInterface, i) ->
                                activity.stopService(new Intent(activity, TimerService.class)))
                        .setNegativeButton(android.R.string.no, (dialogInterface, i) -> {})
                        .create();
                    closeDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    closeDialog.show();
                }
                else {
                    activity.stopService(new Intent(activity, TimerService.class));
                }
            }
            else if (action.equals("action_save_best_time")) {
                long time = intent.getLongExtra("com.example.ronmad.speedruntimer.time", 0);
                activity.updateBestTime(time);
            }
        }
    }

    private static class SnackbarHandler extends Handler {
        Snackbar snackbar;

        SnackbarHandler(Snackbar snackbar) {
            this.snackbar = snackbar;
        }

        @Override
        public void handleMessage(Message msg) {
           switch (msg.what) {
               case SNACKBAR_WHAT:
                   snackbar.show();
           }
        }
    }

    public static class NewGameDialog extends DialogFragment {
        MainActivity activity;
        private LayoutInflater inflater;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            inflater = getActivity().getLayoutInflater();
            if (getTag().equals("NewGameDialog")) {
                return createNewGameDialog();
            }
            if (getTag().equals("NewCategoryDialog")) {
                return createNewCategoryDialog();
            }
            return null;
        }

        private AlertDialog createNewGameDialog() {
            View dialogView = inflater.inflate(R.layout.new_game_dialog, null);
            final EditText newGameInput = (EditText) dialogView.findViewById(R.id.newGameNameInput);
            final MyAutoCompleteTextView newCategoryInput = (MyAutoCompleteTextView) dialogView.findViewById(R.id.newGameCategoryInput);
            return new AlertDialog.Builder(getActivity())
                .setTitle("New game")
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    String newGameName = newGameInput.getText().toString();
                    String newCategory = newCategoryInput.getText().toString();
                    if (newGameName.isEmpty()) {
                        return;
                    }
                    activity.addGameAndCategory(newGameName, newCategory);
                })
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {})
                .create();
        }

        private AlertDialog createNewCategoryDialog() {
            View dialogView = inflater.inflate(R.layout.new_category_dialog, null);
            final MyAutoCompleteTextView newCategoryInput = (MyAutoCompleteTextView) dialogView.findViewById(R.id.newCategoryInput);
            return new AlertDialog.Builder(getActivity())
                .setTitle("New category")
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    String newCategory = newCategoryInput.getText().toString();
                    if (newCategory.isEmpty() || activity.currentGame.hasCategory(newCategory)) {
                        return;
                    }
                    activity.addCategory(newCategory);
                })
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {})
                .create();
        }
    }
}
