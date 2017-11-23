package il.ronmad.speedruntimer;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class Chronometer {

    private TimerService service;
    private View view;

    private TextView chronoRest;
    private TextView chronoMillis;

    static long bestTime = 0;
    static int colorNeutral;
    static int colorAhead;
    static int colorBehind;
    static int colorPB;
    static long countdown;
    static boolean started;
    static boolean running;

    private long base;
    private long timeElapsed;

    private Handler chronoHandler;
    private static final int TICK_WHAT = 2;

    public Chronometer(Context context, View view) {
        service = (TimerService) context;
        this.view = view;

        chronoRest = view.findViewById(R.id.chronoRest);
        chronoMillis = view.findViewById(R.id.chronoMillis);

        chronoHandler = new ChronoHandler(this);

        init();
    }

    private void init() {
        started = false;
        timeElapsed = -1 * countdown;
        setChronoTextFromTime(timeElapsed);
        setColor(colorNeutral);
    }

    public void start() {
        started = true;
        running = true;
        base = SystemClock.elapsedRealtime() - timeElapsed;
        updateRunning();
        if (bestTime == 0) {
            setColor(colorNeutral);
        }
    }

    public void stop() {
        running = false;
        updateRunning();
        if (timeElapsed > 0 && (bestTime == 0 || timeElapsed < bestTime)) {
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
        setChronoTextFromTime(timeElapsed);
        updateColor();
    }

    private void setChronoTextFromTime(long time) {
        int[] units = Util.getTimeUnits(Math.abs(time));
        int hours = units[0], minutes = units[1], seconds = units[2], millis = units[3] / 10;
        if (hours > 0) {
            chronoRest.setText(String.format(Locale.getDefault(),
                    (time < 0) ? "-%d:%02d:%02d" : "%d:%02d:%02d" , hours, minutes, seconds));
        } else {
            chronoRest.setText(String.format(Locale.getDefault(),
                    (time < 0) ? "-%d:%02d" : "%d:%02d", minutes, seconds));
        }
        chronoMillis.setText(String.format(Locale.getDefault(), ".%02d", millis));
    }

    private void updateColor() {
        if (bestTime == 0 || timeElapsed < 0) {
            return;
        }
        if (timeElapsed < bestTime && chronoRest.getCurrentTextColor() != colorAhead) {
            setColor(colorAhead);
        } else if (timeElapsed >= bestTime && chronoRest.getCurrentTextColor() != colorBehind) {
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
        chronoRest.setTextColor(color);
        chronoMillis.setTextColor(color);
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
