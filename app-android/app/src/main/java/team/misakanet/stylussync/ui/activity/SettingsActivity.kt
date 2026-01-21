package team.misakanet.stylussync.ui.activity

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import team.misakanet.stylussync.R
import team.misakanet.stylussync.utils.Constants
import team.misakanet.stylussync.utils.ThemeHelper

/**
 * Activity for displaying application settings.
 */
class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    
    /**
     * Called when the activity is starting.
     * @param savedInstanceState If the activity is being re-created from a previous saved state, this is the state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(preferences)
        super.onCreate(savedInstanceState)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        setContentView(R.layout.activity_settings)
        
        preferences.registerOnSharedPreferenceChangeListener(this)
    }
    
    /**
     * Called before the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }
    
    /**
     * Called when a shared preference is changed.
     * @param sharedPreferences The shared preferences.
     * @param key The key of the changed preference.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Constants.PreferenceKeys.KEY_APP_THEME) {
            sharedPreferences?.let {
                ThemeHelper.applyTheme(it)
                recreate()
            }
        }
    }
}
