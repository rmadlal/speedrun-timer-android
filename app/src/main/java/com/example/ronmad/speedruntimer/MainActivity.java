package com.example.ronmad.speedruntimer;

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
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int SNACKBAR_WHAT = 0;

    private BroadcastReceiver receiver;
    private SharedPreferences sharedPrefs;
    private Gson gson;

    private List<Game> games;

    private Spinner gameSpinner;
    private ArrayAdapter<String> spinnerAdapter;

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
        gson = new Gson();

        gameSpinner = (Spinner) findViewById(R.id.gameSpinner);
        letsGoButton = (Button) findViewById(R.id.startButton);
        deleteGameButton = (ImageButton) findViewById(R.id.deleteGameButton);
        pbText = (TextView) findViewById(R.id.pbText);
        fabAdd = (FloatingActionButton) findViewById(R.id.fabAdd);
        addSnackbar = Snackbar.make(fabAdd, R.string.fab_add_str, Snackbar.LENGTH_INDEFINITE);
        snackbarHandler = new SnackbarHandler(addSnackbar);

        String savedData = sharedPrefs.getString("games", "");
        if (savedData.isEmpty()) {
            games = new ArrayList<>();
        }
        else {
            Game[] gameArr = gson.fromJson(savedData, Game[].class);
            games = new ArrayList<>(Arrays.asList(gameArr));
        }

        setupSpinner();
        checkEmptyGames();

        receiver = new MyReceiver(this);
        IntentFilter intentFilter = new IntentFilter("action_close_timer");
        intentFilter.addAction("save-best-time");
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (pbText != null) {
            handlePbText();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        sharedPrefs.edit()
                   .putString("games", games.isEmpty() ? "" : gson.toJson(games))
                   .putInt("spinnerPos", gameSpinner.getSelectedItemPosition())
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
                games.get(gameSpinner.getSelectedItemPosition()));

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

    private void setupSpinner() {
        List<String> spinnerEntries = new ArrayList<>();
        for (Game game : games) {
            spinnerEntries.add(game.name);
        }
        spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerEntries);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameSpinner.setAdapter(spinnerAdapter);
        gameSpinner.setSelection(sharedPrefs.getInt("spinnerPos", 0));

        gameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                handlePbText();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
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

    public void deleteGameButtonPressed(View view) {
        final int gameIndex = gameSpinner.getSelectedItemPosition();
        Game game = games.get(gameIndex);
        if (game.bestTime > 0) {
            new AlertDialog.Builder(this)
                .setTitle("Delete " + game.name)
                .setMessage("Your PB of " + game.getFormattedBestTime() + " will be deleted. Are you sure?")
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteGame(gameIndex);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .create().show();
        }
        else {
            deleteGame(gameIndex);
        }
    }

    private void checkEmptyGames() {
        gameSpinner.setEnabled(!games.isEmpty());
        deleteGameButton.setEnabled(!games.isEmpty());
        letsGoButton.setEnabled(!games.isEmpty());
        if (games.isEmpty()) {
            spinnerAdapter.add(getString(R.string.no_games));
            deleteGameButton.setColorFilter(ContextCompat.getColor(this, R.color.colorAccentSecondary));
            snackbarHandler.sendEmptyMessageDelayed(SNACKBAR_WHAT, 1000);
            stopService(new Intent(this, TimerService.class));
        }
        else {
            spinnerAdapter.remove(getString(R.string.no_games));
            deleteGameButton.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
            addSnackbar.dismiss();
        }
        spinnerAdapter.notifyDataSetChanged();
    }

    private void handlePbText() {
        if (games.isEmpty()) {
            pbText.setVisibility(View.INVISIBLE);
            return;
        }
        Game game = games.get(gameSpinner.getSelectedItemPosition());
        if (game.bestTime > 0) {
            pbText.setText("PB: " + game.getFormattedBestTime());
            pbText.setVisibility(View.VISIBLE);
        }
        else {
            pbText.setVisibility(View.INVISIBLE);
        }
    }

    private void deleteGame(int gameIndex) {
        spinnerAdapter.remove(games.get(gameIndex).name);
        games.remove(gameIndex);
        checkEmptyGames();
        gameSpinner.setSelection(Math.max(0, gameSpinner.getSelectedItemPosition() - 1));
        handlePbText();
    }

    public void updateBestTime(long time) {
        games.get(gameSpinner.getSelectedItemPosition()).bestTime = time;
        sharedPrefs.edit()
                .putString("games", games.isEmpty() ? "" : gson.toJson(games))
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
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    activity.stopService(new Intent(activity, TimerService.class));
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            })
                            .create();
                    closeDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    closeDialog.show();
                }
                else {
                    activity.stopService(new Intent(activity, TimerService.class));
                }
            }
            else if (action.equals("save-best-time")) {
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

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.new_game_dialog, null);
            final EditText newGameInput = (EditText) dialogView.findViewById(R.id.newGameInput);
            return new AlertDialog.Builder(getActivity())
                .setTitle("New game")
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    String newGameName = newGameInput.getText().toString();
                    if (newGameName.isEmpty()) {
                        return;
                    }
                    activity.games.add(0, new Game(newGameName, 0));
                        activity.spinnerAdapter.insert(newGameName, 0);
                        activity.spinnerAdapter.notifyDataSetChanged();
                        activity.checkEmptyGames();
                        activity.gameSpinner.setSelection(0);
                        activity.handlePbText();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .create();
        }
    }
}
