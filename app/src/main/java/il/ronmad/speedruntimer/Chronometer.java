package il.ronmad.speedruntimer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class Chronometer {

    private TextView chronoMillis;
    private TextView chronoRest;

    static long bestTime;
    static boolean started;
    static boolean running;

    private long base;
    private long timeElapsed;
    private SimpleDateFormat millisDf;
    private SimpleDateFormat restDf;
    private boolean hoursShowing;

    private Handler handler;

    public Chronometer(Context context, View view) {

        chronoMillis = (TextView) view.findViewById(R.id.chronoMillis);
        chronoRest = (TextView) view.findViewById(R.id.chronoRest);

        chronoMillis.setTypeface(Typeface.createFromAsset(
                context.getAssets(), "fonts/digital-7.ttf"));
        chronoRest.setTypeface(Typeface.createFromAsset(
                context.getAssets(), "fonts/digital-7.ttf"));

        millisDf = new SimpleDateFormat(".SS", Locale.getDefault());

        handler = new Handler();

        init();
    }

    private void init() {
        started = false;
        timeElapsed = 0;

        restDf = new SimpleDateFormat("m:ss", Locale.getDefault());
        hoursShowing = false;

        chronoMillis.setText(R.string.chrono_millis);
        chronoRest.setText(R.string.chrono_rest);

        chronoMillis.setTextColor(Color.DKGRAY);
        chronoRest.setTextColor(Color.DKGRAY);
    }

    public void start() {
        started = true;
        running = true;
        base = SystemClock.elapsedRealtime() - timeElapsed;
        update();
    }

    public void stop() {
        running = false;
    }

    public void reset() {
        init();
    }

    public boolean isRunning() {
        return running;
    }

    private void update() {
        timeElapsed = SystemClock.elapsedRealtime() - base;

        int hours = (int) (timeElapsed / (3600 * 1000));
        if (hours > 0) {
            if (!hoursShowing) {
                restDf = new SimpleDateFormat(":mm:ss", Locale.getDefault());
                hoursShowing = true;
            }
            chronoRest.setText(String.format(Locale.getDefault(),
                    "%d%s", hours, restDf.format(timeElapsed)));
        } else {
            chronoRest.setText(restDf.format(timeElapsed));
        }

        chronoMillis.setText(millisDf.format(timeElapsed));
        updateColor();

        if (running) {
            handler.postDelayed(this::update, 15);
        }
    }

    private void updateColor() {
        if (bestTime == 0) {
            return;
        }
        int colorAhead = Color.parseColor("#007000");
        int colorBehind = Color.parseColor("#700000");
        if (timeElapsed < bestTime && chronoMillis.getCurrentTextColor() != colorAhead) {
            chronoMillis.setTextColor(colorAhead);
            chronoRest.setTextColor(colorAhead);

        } else if (timeElapsed >= bestTime && chronoMillis.getCurrentTextColor() != colorBehind) {
            chronoMillis.setTextColor(colorBehind);
            chronoRest.setTextColor(colorBehind);
        }
    }

    public long getTimeElapsed() {
        return timeElapsed;
    }
}
