package team.misakanet.stylussync.utils

/**
 * Contains application-wide constants organized into nested classes.
 */
object Constants {
    
    /**
     * Keys for shared preferences.
     */
    object PreferenceKeys {
        /** Key for template image preference. */
        const val KEY_TEMPLATE_IMAGE = "template_image"
        /** Key for keep display active preference. */
        const val KEY_KEEP_DISPLAY_ACTIVE = "keep_display_active_preference"
        /** Key for stylus only preference. */
        const val KEY_STYLUS_ONLY = "stylus_only_preference"
        /** Key for app theme preference. */
        const val KEY_APP_THEME = "app_theme_preference"
        /** Key for pen color preference. */
        const val KEY_PEN_COLOR = "pen_color_preference"
        /** Key for grid visible preference. */
        const val KEY_GRID_VISIBLE = "grid_visible_preference"
        /** Key for grid opacity preference. */
        const val KEY_GRID_OPACITY = "grid_opacity_preference"
        /** Key for stroke overlay visible preference. */
        const val KEY_STROKE_OVERLAY_VISIBLE = "stroke_overlay_visible_preference"
        /** Key for stroke fade duration preference. */
        const val KEY_STROKE_FADE_DURATION = "stroke_fade_duration_preference"
        /** Key for template opacity preference. */
        const val KEY_TEMPLATE_OPACITY = "template_opacity_preference"
        /** Key for template scale mode preference. */
        const val KEY_TEMPLATE_SCALE_MODE = "template_scale_mode_preference"
    }
    
    /**
     * Values for theme settings.
     */
    object ThemeValues {
        /** System theme value. */
        const val SYSTEM = "system"
        /** Light theme value. */
        const val LIGHT = "light"
        /** Dark theme value. */
        const val DARK = "dark"
    }
    
    /**
     * Request codes for activities and permissions.
     */
    object RequestCodes {
        /** Request code for read storage permission. */
        const val PERMISSION_READ_STORAGE = 100
    }
    
    /**
     * Scale modes for template images.
     */
    object TemplateScaleModes {
        /** Fit center scale mode. */
        const val FIT_CENTER = "fit_center"
        /** Fit XY scale mode. */
        const val FIT_XY = "fit_xy"
        /** Center crop scale mode. */
        const val CENTER_CROP = "center_crop"
        /** Center inside scale mode. */
        const val CENTER_INSIDE = "center_inside"
    }
    
    /**
     * MIDI-related constants.
     */
    object Midi {
        /** Minimum API level for MIDI support. */
        const val MIN_API_LEVEL = 23
        /** Maximum pressure value. */
        const val MAX_PRESSURE = 32768
        /** Maximum coordinate value. */
        const val MAX_COORDINATE = 65535
    }
    
    /**
     * Image processing constants.
     */
    object ImageConfig {
        /** Minimum bitmap sample size. */
        const val BITMAP_SAMPLE_SIZE_MIN = 1
    }
}
