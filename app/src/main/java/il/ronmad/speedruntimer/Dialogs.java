package il.ronmad.speedruntimer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class Dialogs {

    static AlertDialog newGameDialog(MainActivity activity) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.new_game_dialog, null);
        EditText newGameInput = dialogView.findViewById(R.id.newGameNameInput);
        AlertDialog dialog =  new AlertDialog.Builder(activity)
                .setTitle("New game")
                .setView(dialogView)
                .setPositiveButton(R.string.create, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            Button createButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            createButton.setOnClickListener(view -> {
                String newName = newGameInput.getText().toString();
                if (checkGameError(activity, newGameInput)) {
                    newGameInput.requestFocus();
                } else {
                    activity.addGame(newName);
                    dialog.dismiss();
                }
            });
        });
        return dialog;
    }

    static AlertDialog newCategoryDialog(MainActivity activity) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.new_category_dialog, null);
        EditText newCategoryInput = dialogView.findViewById(R.id.newCategoryInput);
        AlertDialog dialog =  new AlertDialog.Builder(activity)
                .setTitle("New category")
                .setView(dialogView)
                .setPositiveButton(R.string.create, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            Button createButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            createButton.setOnClickListener(view -> {
                String newName = newCategoryInput.getText().toString();
                if (checkCategoryError(activity, newCategoryInput)) {
                    newCategoryInput.requestFocus();
                } else {
                    activity.addCategory(newName);
                    dialog.dismiss();
                }
            });
        });
        return dialog;
    }

    static AlertDialog editGameDialog(MainActivity activity, Game game) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.new_game_dialog, null);
        EditText newGameInput = dialogView.findViewById(R.id.newGameNameInput);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Edit name")
                .setView(dialogView)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            Button createButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            createButton.setOnClickListener(view -> {
                String newName = newGameInput.getText().toString();
                if (checkGameError(activity, newGameInput)) {
                    newGameInput.requestFocus();
                } else {
                    activity.editGameName(game, newName);
                    dialog.dismiss();
                }
            });
        });
        return dialog;
    }

    static AlertDialog editCategoryDialog(MainActivity activity, Category category) {
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.edit_category_dialog, null);
        EditText hoursInput = dialogView.findViewById(R.id.hours);
        EditText minutesInput = dialogView.findViewById(R.id.minutes);
        EditText secondsInput = dialogView.findViewById(R.id.seconds);
        EditText millisInput = dialogView.findViewById(R.id.milliseconds);
        EditText runCountInput = dialogView.findViewById(R.id.runCount);
        if (category.bestTime > 0) {
            Util.setEditTextsFromTime(category.bestTime, hoursInput, minutesInput, secondsInput, millisInput);
        }
        runCountInput.setText(String.valueOf(category.runCount));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(String.format("Edit %s %s", activity.currentGame.name, category.name))
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialogInterface, i) -> {
                    long newTime = Util.getTimeFromEditTexts(hoursInput, minutesInput, secondsInput, millisInput);
                    String newRunCountStr = runCountInput.getText().toString();
                    int newRunCount = newRunCountStr.isEmpty() ? 0 : Integer.parseInt(newRunCountStr);
                    activity.editCategory(category, newTime, newRunCount);
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

    static AlertDialog timerActiveDialog(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setMessage("Timer is active. Close anyway?")
                .setPositiveButton(R.string.close, (DialogInterface, i) ->
                        context.stopService(new Intent(context, TimerService.class)))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        return dialog;
    }

    static AlertDialog closeTimerOnResumeDialog(MainActivity activity) {
        return new AlertDialog.Builder(activity)
                .setMessage("Timer must be closed in order to use the app.")
                .setPositiveButton(R.string.close, (dialogInterface, i) ->
                        activity.stopService(new Intent(activity, TimerService.class))
                )
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    activity.startActivity(homeIntent);
                })
                .create();
    }

    static AlertDialog deleteCategoryDialog(MainActivity activity, Category[] toRemove) {
        Category category = toRemove[0];
        return new AlertDialog.Builder(activity)
                .setTitle(String.format("Delete %s %s?", activity.currentGame.name, category.name))
                .setMessage(String.format("Your PB of %s will be lost.", Util.getFormattedTime(category.bestTime)))
                .setPositiveButton(R.string.delete, (dialogInterface, i) -> activity.removeCategories(toRemove))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    static AlertDialog deleteCategoriesDialog(MainActivity activity, Category[] toRemove){
        return new AlertDialog.Builder(activity)
                .setTitle("Delete selected categories?")
                .setMessage("Your PBs will be lost.")
                .setPositiveButton(R.string.delete, (dialogInterface, i) -> activity.removeCategories(toRemove))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }


    static AlertDialog deleteGamesDialog(MainActivity activity, Game[] toRemove) {
        return new AlertDialog.Builder(activity)
                .setTitle("Delete selected games?")
                .setMessage("All categories and PBs associated with the games will be lost.")
                .setPositiveButton(R.string.delete, (dialogInterface, i) -> activity.removeGames(toRemove))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    private static boolean checkGameError(MainActivity activity, EditText editText) {
        String newGameName = editText.getText().toString();
        if (newGameName.isEmpty()) {
            editText.setError(activity.getString(R.string.error_empty_game));
            return true;
        }
        if (activity.games.contains(new Game(newGameName))) {
            editText.setError(activity.getString(R.string.error_game_already_exists));
            return true;
        }
        return false;
    }

    private static boolean checkCategoryError(MainActivity activity, EditText editText) {
        String newCategoryName = editText.getText().toString();
        if (newCategoryName.isEmpty()) {
            editText.setError(activity.getString(R.string.error_empty_category));
            return true;
        } else if (activity.currentGame.hasCategory(newCategoryName)) {
            editText.setError(activity.getString(R.string.error_category_already_exists));
            return true;
        }
        return false;
    }
}
