package com.example.ronmad.speedruntimer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Locale;

class Chronometer extends AppCompatTextView {

    static long bestTime;
    static boolean started;
    static boolean running;

    private long mBase;
    private long timeElapsed;
    private SimpleDateFormat df;
    private boolean hoursShowing;

    private Handler mHandler;
    private static final int TICK_WHAT = 2;

    public Chronometer(Context context) {
        super(context);

        ctor(context);
    }

    public Chronometer(Context context, AttributeSet attrs) {
        super(context, attrs);

        ctor(context);
    }

    public Chronometer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        ctor(context);
    }

    private void ctor(Context context) {
        mHandler = new MyHandler(this);

        setTypeface(Typeface.createFromAsset(
                context.getAssets(), "fonts/digital-7.ttf"));

        init();
    }

    private void init() {
        started = false;
        timeElapsed = 0;

        df = new SimpleDateFormat("m:ss.SS", Locale.getDefault());
        hoursShowing = false;

        setText(R.string.chrono_init);
        setTextColor(Color.DKGRAY);
    }

    public void start() {
        started = true;
        running = true;
        mBase = SystemClock.elapsedRealtime() - timeElapsed;
        updateRunning();
    }

    public void stop() {
        running = false;
        updateRunning();
    }

    public void reset() {
        if (running) return;
        init();
    }

    public boolean isRunning() {
        return running;
    }

    private synchronized void updateText(long now) {
        timeElapsed = now - mBase;

        int hours = (int) (timeElapsed / (3600 * 1000));
        if (hours > 0 && !hoursShowing) {
            df = new SimpleDateFormat("H:mm:ss.SS", Locale.getDefault());
            hoursShowing = true;
        }

        setText(df.format(timeElapsed));
        updateColor();
    }

    private void updateColor() {
        if (bestTime == 0) {
            return;
        }
        if (timeElapsed < bestTime && getCurrentTextColor() != Color.GREEN) {
            setTextColor(Color.parseColor("#009600"));

        }
        else if (timeElapsed >= bestTime && getCurrentTextColor() != Color.RED) {
            setTextColor(Color.parseColor("#960000"));
        }
    }

    private void updateRunning() {
        if (running) {
            updateText(SystemClock.elapsedRealtime());
            mHandler.sendMessageDelayed(Message.obtain(mHandler, TICK_WHAT), 15);
        } else {
            mHandler.removeMessages(TICK_WHAT);
        }
    }

    public long getTimeElapsed() {
        return timeElapsed;
    }

    private static class MyHandler extends Handler {
        private final WeakReference<Chronometer> instance;

        MyHandler(Chronometer instance) {
            this.instance = new WeakReference<>(instance);
        }

        public void handleMessage(Message m) {
            Chronometer mChronometer = instance.get();
            if (mChronometer != null && mChronometer.isRunning()) {
                mChronometer.updateText(SystemClock.elapsedRealtime());
                sendMessageDelayed(Message.obtain(this, TICK_WHAT), 10);
            }
        }
    }
}
