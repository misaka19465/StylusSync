package team.misakanet.stylussync

import android.app.Application
import com.google.android.material.color.DynamicColors

/**
 * The main Application class for the StylusSync app.
 * This class initializes the application and applies dynamic colors.
 */
class StylusSyncApplication : Application() {

    /**
     * Called when the application is starting, before any other application objects have been created.
     * Initializes dynamic colors for the app.
     */
    override fun onCreate() {
        super.onCreate()

        // Apply dynamic colors from wallpaper
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
