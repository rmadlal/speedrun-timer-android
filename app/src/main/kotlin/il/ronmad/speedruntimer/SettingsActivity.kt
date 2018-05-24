package il.ronmad.speedruntimer

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.view.MenuItem
import android.support.v4.app.NavUtils

import com.jaredrummler.android.colorpicker.ColorPreference

class SettingsActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
    }

    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this)
            }
            return true
        }
        return super.onMenuItemSelected(featureId, item)
    }

    override fun onIsMultiPane(): Boolean {
        return isXLargeTablet(this)
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    override fun isValidFragment(fragmentName: String): Boolean {
        return (PreferenceFragment::class.java.name == fragmentName
                || TimerColorsPreferenceFragment::class.java.name == fragmentName
                || TimingPreferenceFragment::class.java.name == fragmentName
                || DisplayPreferenceFragment::class.java.name == fragmentName
                || AppBehaviorPreferenceFragment::class.java.name == fragmentName)
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class TimerColorsPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_timer_colors)
            setHasOptionsMenu(true)

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_neutral)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_ahead)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_behind)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_pb)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_best_segment)))
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                activity.onBackPressed()
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class DisplayPreferenceFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_display)
            setHasOptionsMenu(true)

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_background)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_size)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_show_delta)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_show_current_split)))
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                activity.onBackPressed()
                return true
            }
            return super.onOptionsItemSelected(item)
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class TimingPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_timing)
            setHasOptionsMenu(true)

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_countdown)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_show_millis)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_compare_against)))
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                activity.onBackPressed()
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class AppBehaviorPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_app_behavior)
            setHasOptionsMenu(true)

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_launch_games)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_save_time_data)))
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                activity.onBackPressed()
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    companion object : TimeExtensions {

        private val sBindPreferenceSummaryToValueListener: (Preference, Any) -> Boolean = { preference, value ->
            val stringValue = value.toString()
            when (preference) {
                is ListPreference -> {
                    val index = preference.findIndexOfValue(stringValue)
                    preference.summary =
                            if (index >= 0)
                                preference.entries[index]
                            else null
                }
                is CheckBoxPreference -> {
                    if (preference.key == preference.context.getString(R.string.key_pref_launch_games)) {
                        preference.summary = preference.context.getString(
                                if (value as Boolean)
                                    R.string.pref_launch_games_summary_true
                                else
                                    R.string.pref_launch_games_summary_false)
                    } else if (preference.key == preference.context.getString(R.string.key_pref_save_time_data)) {
                        preference.summary = preference.context.getString(
                                if (value as Boolean)
                                    R.string.pref_save_time_data_summary_true
                                else
                                    R.string.pref_save_time_data_summary_false)
                    }
                }
                is ColorPreference -> {}
                is CountdownPreference ->
                    preference.summary = "Timer starts at ${(-1 * value as Long).getFormattedTime()}"
                else -> preference.summary = stringValue
            }
            true
        }

        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and
                    Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }

        private fun bindPreferenceSummaryToValue(preference: Preference) {
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener)

            val prefs = PreferenceManager
                    .getDefaultSharedPreferences(preference.context)
            when (preference) {
                is ColorPreference ->
                    sBindPreferenceSummaryToValueListener(preference,
                            prefs.getInt(preference.key, 0))
                is CountdownPreference ->
                    sBindPreferenceSummaryToValueListener(preference,
                            prefs.getLong(preference.key, 0L))
                is ListPreference ->
                    sBindPreferenceSummaryToValueListener(preference,
                            prefs.getString(preference.key,
                                    if (preference.key == preference.context.getString(R.string.key_pref_timer_size))
                                        preference.entryValues[1].toString()
                                    else preference.entryValues[0].toString()))
                is CheckBoxPreference ->
                    sBindPreferenceSummaryToValueListener(preference,
                            prefs.getBoolean(preference.key, true))
            }
        }
    }
}
