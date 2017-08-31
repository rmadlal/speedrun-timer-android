package com.example.ronmad.speedruntimer;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
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

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private View mView;
    private Chronometer chronometer;
    Game game;

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

        game = (Game) intent.getSerializableExtra("com.example.ronmad.speedruntimer.game");
        Chronometer.bestTime = game.bestTime;

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.icons8_stopwatch_48)
                .setContentTitle(game.name)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(android.R.drawable.ic_delete, "Close timer", PendingIntent.getBroadcast(
                        this, 0, new Intent("action_close_timer"), PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true);
        if (game.bestTime > 0) {
            notificationBuilder.setContentText("PB: " + game.getFormattedBestTime());
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

        chronometer = (Chronometer) mView.findViewById(R.id.chronometer);

        chronometer.setLongClickable(true);
        chronometer.setOnTouchListener(new View.OnTouchListener() {
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
                        targetX = Math.max(0, targetX);
                        targetX = Math.min(targetX, metrics.widthPixels - v.getWidth());
                        targetY = Math.max(0, targetY);
                        targetY = Math.min(targetY, metrics.heightPixels - v.getHeight());
                        if (!moved &&
                            Math.pow(targetX - initialX, 2) + Math.pow(targetY - initialY, 2) < 20*20)
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

        chronometer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!moved && chronometer.getTimeElapsed() > 0) {
                    if (game.bestTime > 0 && chronometer.getTimeElapsed() >= game.bestTime) {
                        chronometer.reset();
                    }
                    else if (!Chronometer.running) {
                        AlertDialog resetDialog = new AlertDialog.Builder(getApplicationContext())
                            .setTitle("New personal best!")
                            .setMessage("Save it?")
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    game.bestTime = chronometer.getTimeElapsed();
                                    notificationBuilder.setContentText("PB: " + game.getFormattedBestTime());
                                    notificationManager.notify(R.integer.notification_id, notificationBuilder.build());
                                    Intent intent = new Intent("save-best-time");
                                    intent.putExtra("com.example.ronmad.speedruntimer.time", game.bestTime);
                                    sendBroadcast(intent);
                                    chronometer.reset();
                                    Chronometer.bestTime = game.bestTime;
                                }
                            })
                            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    chronometer.reset();
                                }
                            })
                            .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            })
                            .create();
                        resetDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        resetDialog.show();
                    }
                }
                return false;
            }
        });
    }
}
