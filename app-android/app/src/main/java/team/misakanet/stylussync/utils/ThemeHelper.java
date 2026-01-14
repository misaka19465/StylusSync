package team.misakanet.stylussync.utils;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Helper class for managing the application's theme settings.
 */
public class ThemeHelper {
    
    /**
     * Applies the theme based on the preferences.
     * @param preferences The shared preferences instance.
     */
    public static void applyTheme(SharedPreferences preferences) {
        String theme = preferences.getString(Constants.PreferenceKeys.KEY_APP_THEME, 
            Constants.ThemeValues.SYSTEM);
        applyTheme(theme);
    }
    
    /**
     * Applies the theme based on the theme value string.
     * @param themeValue The theme value (system/light/dark).
     */
    public static void applyTheme(String themeValue) {
        switch (themeValue) {
            case Constants.ThemeValues.LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case Constants.ThemeValues.DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case Constants.ThemeValues.SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
