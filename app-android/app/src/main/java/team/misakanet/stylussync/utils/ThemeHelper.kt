package team.misakanet.stylussync.utils

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * Helper class for managing the application's theme settings.
 */
object ThemeHelper {
    
    /**
     * Applies the theme based on the preferences.
     * @param preferences The shared preferences instance.
     */
    @JvmStatic
    fun applyTheme(preferences: SharedPreferences) {
        val theme = preferences.getString(
            Constants.PreferenceKeys.KEY_APP_THEME,
            Constants.ThemeValues.SYSTEM
        ) ?: Constants.ThemeValues.SYSTEM
        applyTheme(theme)
    }
    
    /**
     * Applies the theme based on the theme value string.
     * @param themeValue The theme value (system/light/dark).
     */
    @JvmStatic
    fun applyTheme(themeValue: String) {
        val nightMode = when (themeValue) {
            Constants.ThemeValues.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            Constants.ThemeValues.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
