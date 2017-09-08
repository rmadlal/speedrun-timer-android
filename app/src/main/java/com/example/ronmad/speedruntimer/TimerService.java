package com.example.ronmad.speedruntimer;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class TimerService extends Service {

    public static boolean IS_RUNNING = false;

    private View mView;
    private Chronometer chronometer;
    private long bestTime;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private WindowManager mWindowManager;
    private boolean moved;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IS_RUNNING = true;

        Game game = (Game) intent.getSerializableExtra("com.example.ronmad.speedruntimer.game");
        String category = intent.getStringExtra("com.example.ronmad.speedruntimer.category");
        bestTime = game.getBestTime(category);
        Chronometer.bestTime = bestTime;

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_timer_black_48dp)
                .setContentTitle(game.getName() + " " + category)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(android.R.drawable.ic_delete, "Close timer", PendingIntent.getBroadcast(
                        this, 0, new Intent("action_close_timer"), PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true);
        if (bestTime > 0) {
            notificationBuilder.setContentText("PB: " + Game.getFormattedBestTime(bestTime));
        }
        Notification notification = notificationBuilder.build();
        notificationManager.notify(R.integer.notification_id, notification);
        startForeground(R.integer.notification_id, notification);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        allAboutLayout();
        setupView();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mView != null) {
            mWindowManager.removeView(mView);
        }

        IS_RUNNING = false;
        super.onDestroy();
    }

    WindowManager.LayoutParams mWindowsParams;
    private void setupView() {
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mWindowsParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mWindowsParams.gravity = Gravity.BOTTOM | Gravity.END;
        mWindowManager.addView(mView, mWindowsParams);

    }

    private void allAboutLayout() {
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = layoutInflater.inflate(R.layout.timer_overlay, null);

        chronometer = new Chronometer(this, mView);

        mView.setLongClickable(true);
        mView.setOnTouchListener(new View.OnTouchListener() {
            private DisplayMetrics metrics = new DisplayMetrics();
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            long touchTime;
            long startTime = System.currentTimeMillis();

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (System.currentTimeMillis() - startTime <= 300) {
                    return false;
                }

                mWindowManager.getDefaultDisplay().getMetrics(metrics);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = mWindowsParams.x;
                        initialY = mWindowsParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchTime = System.currentTimeMillis();
                        moved = false;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (moved) break;
                        if (System.currentTimeMillis() - touchTime < 250) {
                            if (chronometer.isRunning()) {
                                chronometer.stop();
                            } else {
                                chronometer.start();
                            }
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int targetX = initialX - (int) (event.getRawX() - initialTouchX);
                        int targetY = initialY - (int) (event.getRawY() - initialTouchY);
                        targetX = Math.max(0, Math.min(targetX, metrics.widthPixels - v.getWidth()));
                        targetY = Math.max(0, Math.min(targetY, metrics.heightPixels - v.getHeight()));
                        if (!moved &&
                            Math.pow(targetX - initialX, 2) + Math.pow(targetY - initialY, 2) < 25*25)
                            break;
                        moved = true;
                        mWindowsParams.x = targetX;
                        mWindowsParams.y = targetY;
                        mWindowManager.updateViewLayout(mView, mWindowsParams);
                        break;
                }
                return false;
            }
        });

        mView.setOnLongClickListener(view -> {
            if (moved || !Chronometer.started || Chronometer.running || chronometer.getTimeElapsed() == 0) {
                return false;
            }
            if (bestTime > 0 && chronometer.getTimeElapsed() >= bestTime) {
                chronometer.reset();
            }
            else if (!Chronometer.running) {
                AlertDialog resetDialog = new AlertDialog.Builder(getApplicationContext())
                    .setTitle("New personal best!")
                    .setMessage("Save it?")
                    .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                        bestTime = chronometer.getTimeElapsed();
                        chronometer.reset();
                        Chronometer.bestTime = bestTime;
                        notificationBuilder.setContentText("PB: " + Game.getFormattedBestTime(bestTime));
                        notificationManager.notify(R.integer.notification_id, notificationBuilder.build());
                        Intent intent = new Intent("action_save_best_time");
                        intent.putExtra("com.example.ronmad.speedruntimer.time", bestTime);
                        sendBroadcast(intent);
                    })
                    .setNegativeButton(R.string.no, (dialogInterface, i) -> chronometer.reset())
                    .setNeutralButton(android.R.string.cancel, null)
                    .create();
                resetDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                resetDialog.show();
            }
            return true;
        });
    }
}
