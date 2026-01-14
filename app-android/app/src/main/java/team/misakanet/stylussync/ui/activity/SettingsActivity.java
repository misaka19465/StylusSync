package team.misakanet.stylussync.ui.activity;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import team.misakanet.stylussync.R;
import team.misakanet.stylussync.utils.Constants;
import team.misakanet.stylussync.utils.ThemeHelper;

/**
 * Activity for displaying application settings.
 */
public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    
    /**
     * Called when the activity is starting.
     * @param savedInstanceState If the activity is being re-created from a previous saved state, this is the state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ThemeHelper.applyTheme(prefs);
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_settings);

        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Called before the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Called when a shared preference is changed.
     * @param sharedPreferences The shared preferences.
     * @param key The key of the changed preference.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Constants.PreferenceKeys.KEY_APP_THEME.equals(key)) {
            ThemeHelper.applyTheme(sharedPreferences);
            recreate();
        }
    }
}
