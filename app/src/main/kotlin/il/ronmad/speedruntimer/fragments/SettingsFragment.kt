package il.ronmad.speedruntimer.fragments

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import il.ronmad.speedruntimer.CountdownPreference
import il.ronmad.speedruntimer.R
import il.ronmad.speedruntimer.TAG_COUNTDOWN_PREFERENCE_FRAGMENT

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        setHasOptionsMenu(true)

        for (listPreferenceKey in arrayOf(
                getString(R.string.key_pref_compare_against),
                getString(R.string.key_pref_timer_size))) {
            findPreference<ListPreference>(listPreferenceKey)?.summaryProvider =
                    ListPreference.SimpleSummaryProvider.getInstance()
        }

        findPreference<CheckBoxPreference>(getString(R.string.key_pref_timer_always_minutes))
                ?.summaryProvider = Preference.SummaryProvider { preference: CheckBoxPreference ->
            preference.context.getString(
                    if (preference.isChecked)
                        R.string.pref_always_show_minutes_summary_true
                    else
                        R.string.pref_always_show_minutes_summary_false)
        }

        findPreference<CheckBoxPreference>(getString(R.string.key_pref_launch_games))
                ?.summaryProvider = Preference.SummaryProvider { preference: CheckBoxPreference ->
            preference.context.getString(
                    if (preference.isChecked)
                        R.string.pref_launch_games_summary_true
                    else
                        R.string.pref_launch_games_summary_false)
        }

        findPreference<CheckBoxPreference>(getString(R.string.key_pref_save_time_data))
                ?.summaryProvider = Preference.SummaryProvider { preference: CheckBoxPreference ->
            preference.context.getString(
                    if (preference.isChecked)
                        R.string.pref_save_time_data_summary_true
                    else
                        R.string.pref_save_time_data_summary_false)
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (parentFragmentManager.findFragmentByTag(TAG_COUNTDOWN_PREFERENCE_FRAGMENT) != null) {
            return
        }

        if (preference is CountdownPreference) {
            val fragment = CountdownPreferenceDialogFragment.newInstance(preference.key)
            fragment.setTargetFragment(this, 0)
            fragment.show(parentFragmentManager, TAG_COUNTDOWN_PREFERENCE_FRAGMENT)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            activity?.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}