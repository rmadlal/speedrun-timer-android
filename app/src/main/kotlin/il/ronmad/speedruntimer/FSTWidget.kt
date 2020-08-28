package il.ronmad.speedruntimer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.RemoteViews
import il.ronmad.speedruntimer.activities.FSTWidgetConfigureActivity
import il.ronmad.speedruntimer.realm.getCategoryByName
import io.realm.Realm

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [FSTWidgetConfigureActivity]
 */
class FSTWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            FSTWidgetConfigureActivity.deleteWidgetPref(context, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created

    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        context ?: return
        when (intent?.action) {
            context.getString(R.string.action_start_timer) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !Settings.canDrawOverlays(context)) {
                    context.showToast(context.getString(R.string.toast_allow_permission), 1)
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")))
                } else {
                    val gameName = intent.getStringExtra(context.getString(R.string.extra_game)).orEmpty()
                    val categoryName = intent.getStringExtra(context.getString(R.string.extra_category)).orEmpty()
                    TimerService.launchTimer(context, gameName, categoryName,
                            minimizeIfNoGameLaunch = false)
                }
            }
        }
    }

    companion object {

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val (gameName, categoryName) = FSTWidgetConfigureActivity.loadWidgetPref(context, appWidgetId)
            val realm = Realm.getDefaultInstance()
            val category = realm.getCategoryByName(gameName, categoryName)
            val pbStr = category?.bestTime?.getFormattedTime() ?: ""

            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.fstwidget)
            views.setTextViewText(R.id.appwidget_text_game, gameName)
            views.setTextViewText(R.id.appwidget_text_category, if (categoryName.isNotEmpty()) categoryName else "FST")
            views.setTextViewText(R.id.appwidget_text_pb, pbStr)

            // Set click listener for starting timer
            val intent = Intent(context, FSTWidget::class.java)
            intent.action = context.getString(R.string.action_start_timer)
            intent.putExtra(context.getString(R.string.extra_game), gameName)
            intent.putExtra(context.getString(R.string.extra_category), categoryName)
            val pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)

            realm.close()
        }

        fun forceUpdateWidgets(context: Context) {
            val intent = Intent(context, FSTWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context.applicationContext)
                    .getAppWidgetIds(ComponentName(context.applicationContext, FSTWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
