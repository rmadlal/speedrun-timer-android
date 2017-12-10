package il.ronmad.speedruntimer;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import com.jaredrummler.android.colorpicker.ColorPreference;

import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
            (preference, value) -> {
                String stringValue = String.valueOf(value);
                if (preference instanceof ListPreference) {
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);
                    preference.setSummary(
                            index >= 0
                                    ? listPreference.getEntries()[index]
                                    : null);
                } else if (preference instanceof CheckBoxPreference) {
                    if (preference.getKey().equals(preference.getContext().getString(R.string.key_pref_launch_games))) {
                        preference.setSummary(preference.getContext().getString((boolean) value ?
                                R.string.pref_launch_games_summary_true
                                : R.string.pref_launch_games_summary_false));
                    }
                } else if (preference instanceof ColorPreference) {
                    return true;
                } else if (preference instanceof CountdownPreference) {
                    preference.setSummary("Timer starts at " + Util.getFormattedTime(-1 * (long) value));
                }  else {
                    preference.setSummary(stringValue);
                }
                return true;
            };

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(preference.getContext());
        if (preference instanceof ColorPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    prefs.getInt(preference.getKey(), 0));
        } else if (preference instanceof CountdownPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    prefs.getLong(preference.getKey(), 0L));
        } else if (preference instanceof ListPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    prefs.getString(preference.getKey(),
                            ((ListPreference) preference).getEntryValues()[1].toString()));
        } else if (preference instanceof CheckBoxPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    prefs.getBoolean(preference.getKey(), true));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || TimerColorsPreferenceFragment.class.getName().equals(fragmentName)
                || TimingPreferenceFragment.class.getName().equals(fragmentName)
                || DisplayPreferenceFragment.class.getName().equals(fragmentName)
                || AppBehaviorPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class TimerColorsPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_timer_colors);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_neutral)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_ahead)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_behind)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_pb)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DisplayPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_display);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_color_background)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_size)));
        }
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class TimingPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_timing);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_countdown)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_timer_show_millis)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AppBehaviorPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_app_behavior);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_pref_launch_games)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
