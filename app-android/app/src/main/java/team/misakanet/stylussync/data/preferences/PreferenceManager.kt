package team.misakanet.stylussync.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import team.misakanet.stylussync.utils.Constants

/**
 * Manages application preferences using SharedPreferences.
 */
class PreferenceManager(context: Context) {

    /** The SharedPreferences instance. */
    val sharedPreferences: SharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * Gets the template image URI.
     * @return The template image URI string, or null if not set.
     */
    fun getTemplateImageUri(): String? {
        return sharedPreferences.getString(Constants.PreferenceKeys.KEY_TEMPLATE_IMAGE, null)
    }

    /**
     * Sets the template image URI.
     * @param uri The template image URI.
     */
    fun setTemplateImageUri(uri: Uri) {
        sharedPreferences.edit().apply {
            putString(Constants.PreferenceKeys.KEY_TEMPLATE_IMAGE, uri.toString())
            apply()
        }
    }

    /**
     * Clears the template image.
     */
    fun clearTemplateImage() {
        sharedPreferences.edit().apply {
            remove(Constants.PreferenceKeys.KEY_TEMPLATE_IMAGE)
            apply()
        }
    }

    /**
     * Checks if the display should be kept active.
     * @return true if the display should stay active, false otherwise.
     */
    fun shouldKeepDisplayActive(): Boolean {
        return sharedPreferences.getBoolean(Constants.PreferenceKeys.KEY_KEEP_DISPLAY_ACTIVE, true)
    }

    /**
     * Checks if only stylus input is accepted.
     * @return true if only stylus input is accepted, false otherwise.
     */
    fun isStylusOnly(): Boolean {
        return sharedPreferences.getBoolean(Constants.PreferenceKeys.KEY_STYLUS_ONLY, false)
    }

    /**
     * Gets the app theme preference.
     * @return The theme value (system/light/dark).
     */
    fun getAppTheme(): String {
        return sharedPreferences.getString(
            Constants.PreferenceKeys.KEY_APP_THEME,
            Constants.ThemeValues.SYSTEM
        ) ?: Constants.ThemeValues.SYSTEM
    }
}
