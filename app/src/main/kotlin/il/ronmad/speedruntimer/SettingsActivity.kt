package il.ronmad.speedruntimer

import android.os.Bundle
import android.preference.*
import android.view.MenuItem
import android.support.v4.app.NavUtils

import com.jaredrummler.android.colorpicker.ColorPreference

class SettingsActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, MyPreferenceFragment())
                .commit()
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

    class MyPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.settings_preferences)
            setHasOptionsMenu(true)

            // Timer colors
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_neutral)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_ahead)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_behind)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_pb)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_best_segment)))

            // Display
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_background)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_size)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_show_millis)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_always_minutes)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_show_delta)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_show_current_split)))

            // Timing
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_countdown)))
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_compare_against)))

            // App behavior
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

    companion object {

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
                    preference.summary = when (preference.key) {
                        preference.context.getString(R.string.key_pref_launch_games) -> {
                            preference.context.getString(
                                    if (value as Boolean)
                                        R.string.pref_launch_games_summary_true
                                    else
                                        R.string.pref_launch_games_summary_false)
                        }
                        preference.context.getString(R.string.key_pref_save_time_data) -> {
                            preference.context.getString(
                                    if (value as Boolean)
                                        R.string.pref_save_time_data_summary_true
                                    else
                                        R.string.pref_save_time_data_summary_false)
                        }
                        preference.context.getString(R.string.key_pref_timer_always_minutes) -> {
                            preference.context.getString(
                                    if (value as Boolean)
                                        R.string.pref_always_show_minutes_summary_true
                                    else
                                        R.string.pref_always_show_minutes_summary_false)
                        }
                        else -> ""
                    }
                }
                is ColorPreference -> {}
                is CountdownPreference ->
                    preference.summary = "Timer starts at ${(-(value as Long)).getFormattedTime()}"
                else -> preference.summary = stringValue
            }
            true
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
