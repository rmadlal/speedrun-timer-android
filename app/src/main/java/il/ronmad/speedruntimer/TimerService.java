package il.ronmad.speedruntimer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import static il.ronmad.speedruntimer.Util.gson;

public class TimerService extends Service {

    public static boolean IS_ACTIVE = false;

    private SharedPreferences prefs;

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
        IS_ACTIVE = true;

        Game game = gson.fromJson(intent.getStringExtra(getString(R.string.game)), Game.class);
        String category = intent.getStringExtra(getString(R.string.category_name));
        bestTime = game.getBestTime(category);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_timer_black_48dp)
                .setContentTitle(String.format("%s %s", game.getName(), category))
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_close_black_24dp, getString(R.string.close_timer),
                        PendingIntent.getBroadcast(this, 0,
                                new Intent(getString(R.string.action_close_timer)),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true);
        if (bestTime > 0) {
            notificationBuilder.setContentText(String.format("Best time: %s", Game.getFormattedBestTime(bestTime)));
        }
        Notification notification = notificationBuilder.build();
        notificationManager.notify(R.integer.notification_id, notification);
        startForeground(R.integer.notification_id, notification);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Chronometer.bestTime = bestTime;
        Chronometer.colorNeutral = prefs.getInt(getString(R.string.key_color_neutral),
                ContextCompat.getColor(this, R.color.timerNeutralColorDefault));
        Chronometer.colorAhead = prefs.getInt(getString(R.string.key_color_ahead),
                ContextCompat.getColor(this, R.color.timerAheadColorDefault));
        Chronometer.colorBehind = prefs.getInt(getString(R.string.key_color_behind),
                ContextCompat.getColor(this, R.color.timerBehindColorDefault));
        Chronometer.colorPB = prefs.getInt(getString(R.string.key_color_pb),
                ContextCompat.getColor(this, R.color.timerPBColorDefault));

        setupLayoutComponents();
        setupView();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mView != null) {
            prefs.edit()
                    .putInt(getString(R.string.key_timer_pos_x), mWindowsParams.x)
                    .putInt(getString(R.string.key_timer_pos_y), mWindowsParams.y)
                    .apply();
            mWindowManager.removeView(mView);
        }

        IS_ACTIVE = false;
        super.onDestroy();
    }

    private void setupLayoutComponents() {
        setTheme(R.style.AppTheme);
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

        mView.setOnLongClickListener(new View.OnLongClickListener() {
             @Override
             public boolean onLongClick(View view) {
                 if (moved || !Chronometer.started || Chronometer.running || chronometer.getTimeElapsed() == 0) {
                     return false;
                 }
                 if (bestTime > 0 && chronometer.getTimeElapsed() >= bestTime) {
                     chronometer.reset();
                 } else if (!Chronometer.running) {
                     AlertDialog resetDialog = new AlertDialog.Builder(TimerService.this)
                             .setTitle(bestTime == 0 ? "New personal best!" :
                                     String.format("New personal best! (-%s)",
                                     Game.getFormattedBestTime(bestTime - chronometer.getTimeElapsed())))
                             .setMessage("Save it?")
                             .setPositiveButton(R.string.save_reset, new DialogInterface.OnClickListener() {
                                 @Override
                                 public void onClick(DialogInterface dialogInterface, int i) {
                                     bestTime = chronometer.getTimeElapsed();
                                     chronometer.reset();
                                     Chronometer.bestTime = bestTime;
                                     notificationBuilder.setContentText(String.format("Personal best: %s", Game.getFormattedBestTime(bestTime)));
                                     notificationManager.notify(R.integer.notification_id, notificationBuilder.build());
                                     Intent intent = new Intent(getString(R.string.action_save_best_time));
                                     intent.putExtra(getString(R.string.best_time), bestTime);
                                     sendBroadcast(intent);
                                 }
                             })
                             .setNegativeButton(R.string.reset, new DialogInterface.OnClickListener() {
                                 @Override
                                 public void onClick(DialogInterface dialogInterface, int i) {
                                     chronometer.reset();
                                 }
                             })
                             .setNeutralButton(android.R.string.cancel, null)
                             .create();
                     resetDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                     resetDialog.show();
                 }
                 return true;
        }});
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
        if (prefs.contains(getString(R.string.key_timer_pos_x))) {
            int x = prefs.getInt(getString(R.string.key_timer_pos_x), 0);
            int y = prefs.getInt(getString(R.string.key_timer_pos_y), 0);
            mWindowsParams.x = Math.max(0, Math.min(x, metrics.widthPixels - mWindowsParams.width));
            mWindowsParams.y = Math.max(0, Math.min(y, metrics.heightPixels - mWindowsParams.height));
        }
        mWindowManager.addView(mView, mWindowsParams);
    }
}
