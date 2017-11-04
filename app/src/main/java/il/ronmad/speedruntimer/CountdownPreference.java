package il.ronmad.speedruntimer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class CountdownPreference extends DialogPreference {

    private long countdown;
    private EditText hoursInput;
    private EditText minutesInput;
    private EditText secondsInput;
    private EditText millisInput;

    public CountdownPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CountdownPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        countdown = 0L;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.edit_time_dialog, null);
        hoursInput = view.findViewById(R.id.hours);
        minutesInput = view.findViewById(R.id.minutes);
        secondsInput = view.findViewById(R.id.seconds);
        millisInput = view.findViewById(R.id.milliseconds);
        Util.setEditTextsFromTime(countdown,
                hoursInput, minutesInput, secondsInput, millisInput);
        builder.setView(view)
                .setPositiveButton(R.string.save, (dialogInterface, i) ->  {
                    countdown = Util.getTimeFromEditTexts(hoursInput, minutesInput, secondsInput, millisInput);
                    persistLong(countdown);
                    setSummary("Timer starts at " + Util.getFormattedTime(-1 * countdown));
                })
                .setNegativeButton(R.string.pb_clear, this)
                .setNeutralButton(android.R.string.cancel, this);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(view -> {
            hoursInput.setText("");
            minutesInput.setText("");
            secondsInput.setText("");
            millisInput.setText("");
        });
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (long) a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        countdown = (restorePersistedValue ?
                getPersistedLong(countdown) : (long) defaultValue);
    }
}
