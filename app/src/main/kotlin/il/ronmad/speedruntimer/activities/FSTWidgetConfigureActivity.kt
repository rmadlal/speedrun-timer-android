package il.ronmad.speedruntimer.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.google.android.material.snackbar.Snackbar
import il.ronmad.speedruntimer.FSTWidget
import il.ronmad.speedruntimer.databinding.FstwidgetConfigureBinding
import il.ronmad.speedruntimer.realm.Game
import il.ronmad.speedruntimer.realm.getAllGames
import io.realm.Realm

/**
 * The configuration screen for the [FSTWidget] AppWidget.
 */
class FSTWidgetConfigureActivity : Activity() {
    private lateinit var viewBinding: FstwidgetConfigureBinding

    private var mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var realm: Realm
    private var games: List<Game> = emptyList()

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        realm = Realm.getDefaultInstance()
        games = realm.getAllGames()

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        viewBinding = FstwidgetConfigureBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setupSpinners()
        viewBinding.addButton.setOnClickListener {
            if (viewBinding.appwidgetSpinnerGame.selectedItem == null
                || viewBinding.appwidgetSpinnerCategory.selectedItem == null
            ) {
                Snackbar.make(viewBinding.addButton, "You must choose a game and category.", Snackbar.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            val gameName = viewBinding.appwidgetSpinnerGame.selectedItem.toString()
            val categoryName = viewBinding.appwidgetSpinnerCategory.selectedItem.toString()
            saveWidgetPref(this, mAppWidgetId, gameName to categoryName)

            // It is the responsibility of the configuration activity to update the app widget
            val appWidgetManager = AppWidgetManager.getInstance(this)
            FSTWidget.updateAppWidget(this, appWidgetManager, mAppWidgetId)

            // Make sure we pass back the original appWidgetId
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }

        // Find the widget id from the intent.
        intent.extras?.let {
            mAppWidgetId = it.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
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
        viewBinding.appwidgetSpinnerGame.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            games.map { it.name }
        )
        viewBinding.appwidgetSpinnerGame.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewBinding.appwidgetSpinnerCategory.adapter = ArrayAdapter(this@FSTWidgetConfigureActivity,
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
        internal fun saveWidgetPref(
            context: Context,
            appWidgetId: Int,
            gameAndCategoryNames: Pair<String, String>
        ) {
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
            return prefs.getString(PREF_PREFIX_KEY + appWidgetId + PREF_GAME_KEY, "")!! to
                    prefs.getString(PREF_PREFIX_KEY + appWidgetId + PREF_CATEGORY_KEY, "")!!
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
