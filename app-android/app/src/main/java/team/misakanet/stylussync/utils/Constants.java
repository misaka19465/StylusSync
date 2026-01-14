package team.misakanet.stylussync.utils;

/**
 * Contains application-wide constants organized into nested classes.
 */
public final class Constants {
    
    // Prevent instantiation
    private Constants() {
        throw new AssertionError("Cannot instantiate Constants class");
    }
    
    /**
     * Keys for shared preferences.
     */
    public static final class PreferenceKeys {
        /** Key for template image preference. */
        public static final String KEY_TEMPLATE_IMAGE = "template_image";
        /** Key for keep display active preference. */
        public static final String KEY_KEEP_DISPLAY_ACTIVE = "keep_display_active_preference";
        /** Key for stylus only preference. */
        public static final String KEY_STYLUS_ONLY = "stylus_only_preference";
        /** Key for app theme preference. */
        public static final String KEY_APP_THEME = "app_theme_preference";
        /** Key for pen color preference. */
        public static final String KEY_PEN_COLOR = "pen_color_preference";
        /** Key for grid visible preference. */
        public static final String KEY_GRID_VISIBLE = "grid_visible_preference";
        /** Key for grid opacity preference. */
        public static final String KEY_GRID_OPACITY = "grid_opacity_preference";
        /** Key for stroke overlay visible preference. */
        public static final String KEY_STROKE_OVERLAY_VISIBLE = "stroke_overlay_visible_preference";
        /** Key for stroke fade duration preference. */
        public static final String KEY_STROKE_FADE_DURATION = "stroke_fade_duration_preference";
        /** Key for template opacity preference. */
        public static final String KEY_TEMPLATE_OPACITY = "template_opacity_preference";
        /** Key for template scale mode preference. */
        public static final String KEY_TEMPLATE_SCALE_MODE = "template_scale_mode_preference";
        
        private PreferenceKeys() {}
    }
    
    /**
     * Values for theme settings.
     */
    public static final class ThemeValues {
        /** System theme value. */
        public static final String SYSTEM = "system";
        /** Light theme value. */
        public static final String LIGHT = "light";
        /** Dark theme value. */
        public static final String DARK = "dark";
        
        private ThemeValues() {}
    }
    
    /**
     * Request codes for activities and permissions.
     */
    public static final class RequestCodes {
        /** Request code for read storage permission. */
        public static final int PERMISSION_READ_STORAGE = 100;
        
        private RequestCodes() {}
    }
    
    /**
     * Scale modes for template images.
     */
    public static final class TemplateScaleModes {
        /** Fit center scale mode. */
        public static final String FIT_CENTER = "fit_center";
        /** Fit XY scale mode. */
        public static final String FIT_XY = "fit_xy";
        /** Center crop scale mode. */
        public static final String CENTER_CROP = "center_crop";
        /** Center inside scale mode. */
        public static final String CENTER_INSIDE = "center_inside";
        
        private TemplateScaleModes() {}
    }
    
    /**
     * MIDI-related constants.
     */
    public static final class Midi {
        /** Minimum API level for MIDI support. */
        public static final int MIN_API_LEVEL = 23;
        /** Maximum pressure value. */
        public static final int MAX_PRESSURE = 32768;
        /** Maximum coordinate value. */
        public static final int MAX_COORDINATE = 65535;
        
        private Midi() {}
    }
    
    /**
     * Image processing constants.
     */
    public static final class ImageConfig {
        /** Minimum bitmap sample size. */
        public static final int BITMAP_SAMPLE_SIZE_MIN = 1;
        
        private ImageConfig() {}
    }
}
