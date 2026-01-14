package team.misakanet.stylussync.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import team.misakanet.stylussync.utils.Constants;

/**
 * Manages application preferences using SharedPreferences.
 */
public class PreferenceManager {

    /** The SharedPreferences instance. */
    private final SharedPreferences sharedPreferences;

    /**
     * Constructs a PreferenceManager with the given context.
     * @param context The application context.
     */
    public PreferenceManager(Context context) {
        this.sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Gets the SharedPreferences instance.
     * @return The SharedPreferences instance.
     */
    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    /**
     * Gets the template image URI.
     * @return The template image URI string, or null if not set.
     */
    public String getTemplateImageUri() {
        return sharedPreferences.getString(Constants.PreferenceKeys.KEY_TEMPLATE_IMAGE, null);
    }

    /**
     * Sets the template image URI.
     * @param uri The template image URI.
     */
    public void setTemplateImageUri(Uri uri) {
        sharedPreferences.edit()
            .putString(Constants.PreferenceKeys.KEY_TEMPLATE_IMAGE, uri.toString())
            .apply();
    }

    /**
     * Clears the template image.
     */
    public void clearTemplateImage() {
        sharedPreferences.edit()
            .remove(Constants.PreferenceKeys.KEY_TEMPLATE_IMAGE)
            .apply();
    }

    /**
     * Checks if the display should be kept active.
     * @return true if the display should stay active, false otherwise.
     */
    public boolean shouldKeepDisplayActive() {
        return sharedPreferences.getBoolean(Constants.PreferenceKeys.KEY_KEEP_DISPLAY_ACTIVE, true);
    }

    /**
     * Checks if only stylus input is accepted.
     * @return true if only stylus input is accepted, false otherwise.
     */
    public boolean isStylusOnly() {
        return sharedPreferences.getBoolean(Constants.PreferenceKeys.KEY_STYLUS_ONLY, false);
    }

    /**
     * Gets the app theme preference.
     * @return The theme value (system/light/dark).
     */
    public String getAppTheme() {
        return sharedPreferences.getString(Constants.PreferenceKeys.KEY_APP_THEME,
            Constants.ThemeValues.SYSTEM);
    }
}
