package il.ronmad.speedruntimer.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import il.ronmad.speedruntimer.FSTWidget
import il.ronmad.speedruntimer.R
import il.ronmad.speedruntimer.realm.Game
import il.ronmad.speedruntimer.realm.getAllGames
import io.realm.Realm
import kotlinx.android.synthetic.main.fstwidget_configure.*

/**
 * The configuration screen for the [FSTWidget] AppWidget.
 */
class FSTWidgetConfigureActivity : Activity() {
    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var realm: Realm
    private var games: List<Game> = emptyList()

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        realm = Realm.getDefaultInstance()
        games = realm.getAllGames()

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(Activity.RESULT_CANCELED)

        setContentView(R.layout.fstwidget_configure)

        setupSpinners()
        add_button.setOnClickListener {
            val context = this@FSTWidgetConfigureActivity

            if (appwidget_spinner_game.selectedItem == null
                    || appwidget_spinner_category.selectedItem == null) {
                Snackbar.make(add_button, "You must choose a game and category.", Snackbar.LENGTH_SHORT)
                        .show()
                return@setOnClickListener
            }
            val gameName = appwidget_spinner_game.selectedItem.toString()
            val categoryName = appwidget_spinner_category.selectedItem.toString()
            saveWidgetPref(context, mAppWidgetId, gameName to categoryName)

            // It is the responsibility of the configuration activity to update the app widget
            val appWidgetManager = AppWidgetManager.getInstance(context)
            FSTWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId)

            // Make sure we pass back the original appWidgetId
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }

        // Find the widget id from the intent.
        intent.extras?.let {
            mAppWidgetId = it.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    private fun setupSpinners() {
        appwidget_spinner_game.adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item,
                games.map { it.name }
        )
        appwidget_spinner_game.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) { }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                appwidget_spinner_category.adapter = ArrayAdapter(this@FSTWidgetConfigureActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        games[position].categories.map { it.name }
                )
            }
        }
    }

    companion object {

        private const val PREFS_NAME = "il.ronmad.speedruntimer.FSTWidget"
        private const val PREF_PREFIX_KEY = "appwidget_"
        private const val PREF_GAME_KEY = "_game"
        private const val PREF_CATEGORY_KEY = "_category"

        // Write the prefix to the SharedPreferences object for this widget
        internal fun saveWidgetPref(context: Context,
                                    appWidgetId: Int,
                                    gameAndCategoryNames: Pair<String, String>) {
            val (gameName, categoryName) = gameAndCategoryNames
            context.getSharedPreferences(PREFS_NAME, 0)
                    .edit()
                    .putString(PREF_PREFIX_KEY + appWidgetId + PREF_GAME_KEY, gameName)
                    .putString(PREF_PREFIX_KEY + appWidgetId + PREF_CATEGORY_KEY, categoryName)
                    .apply()
        }



        // Read the prefix from the SharedPreferences object for this widget.
        // If there is no preference saved, get the default from a resource
        internal fun loadWidgetPref(context: Context, appWidgetId: Int): Pair<String, String> {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getString(PREF_PREFIX_KEY + appWidgetId + PREF_GAME_KEY, "") to
                    prefs.getString(PREF_PREFIX_KEY + appWidgetId + PREF_CATEGORY_KEY, "")
        }

        internal fun deleteWidgetPref(context: Context, appWidgetId: Int) {
            context.getSharedPreferences(PREFS_NAME, 0)
                    .edit()
                    .remove(PREF_PREFIX_KEY + appWidgetId + PREF_GAME_KEY)
                    .remove(PREF_PREFIX_KEY + appWidgetId + PREF_CATEGORY_KEY)
                    .apply()
        }
    }
}
