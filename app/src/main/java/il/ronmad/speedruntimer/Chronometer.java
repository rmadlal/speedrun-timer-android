package il.ronmad.speedruntimer;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Chronometer {

    private TextView chronoMillis;
    private TextView chronoRest;

    static long bestTime = 0;
    static int colorNeutral;
    static int colorAhead;
    static int colorBehind;
    static int colorPB;
    static boolean started;
    static boolean running;

    private long base;
    private long timeElapsed;
    private SimpleDateFormat millisDf;
    private SimpleDateFormat restDf;
    private boolean hoursShowing;

    private Handler chronoHandler;
    private static final int TICK_WHAT = 2;

    public Chronometer(Context context, View view) {
        chronoMillis = (TextView) view.findViewById(R.id.chronoMillis);
        chronoRest = (TextView) view.findViewById(R.id.chronoRest);

        chronoMillis.setTypeface(Typeface.createFromAsset(
                context.getAssets(), "fonts/digital-7.ttf"));
        chronoRest.setTypeface(Typeface.createFromAsset(
                context.getAssets(), "fonts/digital-7.ttf"));

        millisDf = new SimpleDateFormat(".SS", Locale.getDefault());

        chronoHandler = new ChronoHandler(this);

        init();
    }

    private void init() {
        started = false;
        timeElapsed = 0;

        restDf = new SimpleDateFormat("m:ss", Locale.getDefault());
        hoursShowing = false;

        chronoMillis.setText(R.string.chrono_millis);
        chronoRest.setText(R.string.chrono_rest);

        setColor(colorNeutral);
    }

    public void start() {
        started = true;
        running = true;
        base = SystemClock.elapsedRealtime() - timeElapsed;
        updateRunning();
    }

    public void stop() {
        running = false;
        updateRunning();
        if (bestTime == 0 || timeElapsed < bestTime) {
            setColor(colorPB);
        }
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
    }

    private void updateColor() {
        if (bestTime == 0) {
            return;
        }
        if (timeElapsed < bestTime && chronoMillis.getCurrentTextColor() != colorAhead) {
            setColor(colorAhead);

        } else if (timeElapsed >= bestTime && chronoMillis.getCurrentTextColor() != colorBehind) {
            setColor(colorBehind);
        }
    }

    private void updateRunning() {
        if (running) {
            update();
            chronoHandler.sendMessageDelayed(Message.obtain(chronoHandler, TICK_WHAT), 15);
        } else {
            chronoHandler.removeMessages(TICK_WHAT);
        }
    }

    public long getTimeElapsed() {
        return timeElapsed;
    }

    public void setColor(int color) {
        chronoMillis.setTextColor(color);
        chronoRest.setTextColor(color);
    }

    private static class ChronoHandler extends Handler {
        private final WeakReference<Chronometer> instance;

        ChronoHandler(Chronometer instance) {
            this.instance = new WeakReference<>(instance);
        }

        public void handleMessage(Message m) {
            Chronometer mChronometer = instance.get();
            if (mChronometer != null && mChronometer.isRunning()) {
                mChronometer.update();
                sendMessageDelayed(Message.obtain(this, TICK_WHAT), 15);
            }
        }
    }
}
