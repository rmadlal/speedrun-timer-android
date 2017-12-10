package il.ronmad.speedruntimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import io.realm.Realm;
import io.realm.RealmChangeListener;

public class TimerService extends Service {

    public static boolean IS_ACTIVE = false;

    private Realm realm;
    private RealmChangeListener<Realm> realmChangeListener;
    private SharedPreferences prefs;
    private String gameName;
    private String categoryName;
    private Game game;
    private Category category;

    private View mView;
    private Chronometer chronometer;

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
        realm = Realm.getDefaultInstance();
        realmChangeListener = realm -> onDataChange();
        realm.addChangeListener(realmChangeListener);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IS_ACTIVE = true;

        gameName = intent.getStringExtra(getString(R.string.extra_game));
        categoryName = intent.getStringExtra(getString(R.string.extra_category));
        game = realm.where(Game.class).equalTo("name", gameName).findFirst();
        category = game.getCategory(categoryName);

        Notification notification = setupNotification();
        startForeground(R.integer.notification_id, notification);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Chronometer.bestTime = category.bestTime;
        Chronometer.colorNeutral = prefs.getInt(getString(R.string.key_pref_color_neutral),
                ContextCompat.getColor(this, R.color.colorTimerNeutralDefault));
        Chronometer.colorAhead = prefs.getInt(getString(R.string.key_pref_color_ahead),
                ContextCompat.getColor(this, R.color.colorTimerAheadDefault));
        Chronometer.colorBehind = prefs.getInt(getString(R.string.key_pref_color_behind),
                ContextCompat.getColor(this, R.color.colorTimerBehindDefault));
        Chronometer.colorPB = prefs.getInt(getString(R.string.key_pref_color_pb),
                ContextCompat.getColor(this, R.color.colorTimerPBDefault));
        Chronometer.countdown = prefs.getLong(getString(R.string.key_pref_timer_countdown), 0L);
        Chronometer.showMillis = prefs.getBoolean(getString(R.string.key_pref_timer_show_millis), true);

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        setupLayoutComponents();
        setupView();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        realm.removeChangeListener(realmChangeListener);
        if (mView != null) {
            if (game != null) {
                game.timerPosition.set(mWindowParams.x, mWindowParams.y);
            }
            mWindowManager.removeView(mView);
        }
        realm.close();
        IS_ACTIVE = false;
        super.onDestroy();
    }

    private void onDataChange() {
        Chronometer.bestTime = category.bestTime;
        notificationBuilder.setContentText(category.bestTime > 0 ?
                String.format("PB: %s", Util.getFormattedTime(category.bestTime))
                : null);
        notificationManager.notify(R.integer.notification_id, notificationBuilder.build());
    }

    private Notification setupNotification() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
                .setSmallIcon(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                        R.drawable.ic_timer_black_48dp : R.drawable.ic_stat_timer)
                .setContentTitle(String.format("%s %s", game.name, category.name))
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                                R.drawable.ic_close_black_24dp : R.drawable.ic_stat_close,
                        getString(R.string.close_timer),
                        PendingIntent.getBroadcast(this, 0,
                                new Intent(getString(R.string.action_close_timer)),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true);
        if (category.bestTime > 0) {
            notificationBuilder.setContentText(String.format("PB: %s", Util.getFormattedTime(category.bestTime)));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    getString(R.string.notification_channel_id),
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW);
            notificationChannel.enableVibration(false);
            notificationChannel.enableLights(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        Notification notification = notificationBuilder.build();
        notificationManager.notify(R.integer.notification_id, notification);
        return notification;
    }

    private void setupLayoutComponents() {
        setTheme(R.style.AppTheme);
        mView = View.inflate(this, R.layout.timer_overlay, null);

        mView.setBackgroundColor(prefs.getInt(getString(R.string.key_pref_color_background),
                ContextCompat.getColor(this, R.color.colorTimerBackgroundDefault)));
        int size = Integer.parseInt(prefs.getString(getString(R.string.key_pref_timer_size), "32"));
        TextView chronoRest = mView.findViewById(R.id.chronoRest);
        TextView chronoMillis = mView.findViewById(R.id.chronoMillis);
        chronoRest.setTextSize(size);
        chronoMillis.setTextSize((float) (size * 0.75));

        chronoRest.setTypeface(Typeface.createFromAsset(
                getAssets(), "fonts/digital-7.ttf"));
        chronoMillis.setTypeface(Typeface.createFromAsset(
                getAssets(), "fonts/digital-7.ttf"));

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
                        initialX = mWindowParams.x;
                        initialY = mWindowParams.y;
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
                        mWindowParams.x = targetX;
                        mWindowParams.y = targetY;
                        mWindowManager.updateViewLayout(mView, mWindowParams);
                        break;
                }
                v.performClick();
                return false;
            }
        });

        mView.setOnLongClickListener(view -> {
            long time = chronometer.getTimeElapsed();
            if (moved || !Chronometer.started || Chronometer.running || time == 0) {
                return false;
            }
            if (time < 0) {
                chronometer.reset();
            } else if (category.bestTime > 0 && time >= category.bestTime) {
                chronometer.reset();
                category.incrementRunCount();
            } else if (!Chronometer.running) {
                AlertDialog resetDialog = new AlertDialog.Builder(TimerService.this)
                        .setTitle(category.bestTime == 0 ? "New personal best!" :
                                String.format("New personal best! (%s)",
                                        Util.getFormattedTime(time - category.bestTime)))
                        .setMessage("Save it?")
                        .setPositiveButton(R.string.save_reset, (dialogInterface, i) -> {
                            chronometer.reset();
                            category.setData(time, category.runCount + 1);
                        })
                        .setNegativeButton(R.string.reset, (dialogInterface, i) ->  {
                            chronometer.reset();
                            category.incrementRunCount();
                        })
                        .setNeutralButton(android.R.string.cancel, null)
                        .create();
                Window window = resetDialog.getWindow();
                if (window != null) {
                    window.setType(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                }
                resetDialog.show();
            }
            return true;
        });
    }

    WindowManager.LayoutParams mWindowParams;
    private void setupView() {
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mWindowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mWindowParams.gravity = Gravity.BOTTOM | Gravity.END;

        int x = game.getTimerPosition().x;
        int y = game.getTimerPosition().y;
        mWindowParams.x = Math.max(0, Math.min(x, metrics.widthPixels - mWindowParams.width));
        mWindowParams.y = Math.max(0, Math.min(y, metrics.heightPixels - mWindowParams.height));
        mWindowManager.addView(mView, mWindowParams);
    }
}
