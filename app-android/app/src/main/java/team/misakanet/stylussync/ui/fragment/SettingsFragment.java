package team.misakanet.stylussync.ui.fragment;

import android.os.Bundle;

import androidx.preference.CheckBoxPreference;
import androidx.preference.DropDownPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;

import team.misakanet.stylussync.R;
import team.misakanet.stylussync.utils.Constants;

/**
 * Fragment for displaying and managing application settings.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    /**
     * Called during onCreate to supply the preferences for this fragment.
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     * @param rootKey If non-null, this preference fragment should be rooted at the PreferenceScreen with this key.
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getContext());

        // System preferences category
        PreferenceCategory systemCategory = new PreferenceCategory(getContext());
        systemCategory.setTitle(R.string.preferences_category_system);
        screen.addPreference(systemCategory);

        // Keep display active preference
        CheckBoxPreference keepDisplayActivePref = new CheckBoxPreference(getContext());
        keepDisplayActivePref.setKey(Constants.PreferenceKeys.KEY_KEEP_DISPLAY_ACTIVE);
        keepDisplayActivePref.setTitle(R.string.preferences_keep_display_active);
        keepDisplayActivePref.setSummaryOn(R.string.preferences_keep_display_active_on);
        keepDisplayActivePref.setSummaryOff(R.string.preferences_keep_display_active_off);
        keepDisplayActivePref.setDefaultValue(true);
        systemCategory.addPreference(keepDisplayActivePref);

        // Drawing preferences category
        PreferenceCategory drawingCategory = new PreferenceCategory(getContext());
        drawingCategory.setTitle(R.string.preferences_category_drawing);
        screen.addPreference(drawingCategory);

        // Stylus only preference
        CheckBoxPreference stylusOnlyPref = new CheckBoxPreference(getContext());
        stylusOnlyPref.setKey(Constants.PreferenceKeys.KEY_STYLUS_ONLY);
        stylusOnlyPref.setTitle(R.string.preferences_stylus_only);
        stylusOnlyPref.setSummaryOn(R.string.preferences_stylus_only_on);
        stylusOnlyPref.setSummaryOff(R.string.preferences_stylus_only_off);
        stylusOnlyPref.setDefaultValue(false);
        drawingCategory.addPreference(stylusOnlyPref);

        // Stroke overlay preference
        CheckBoxPreference strokeOverlayPref = new CheckBoxPreference(getContext());
        strokeOverlayPref.setKey(Constants.PreferenceKeys.KEY_STROKE_OVERLAY_VISIBLE);
        strokeOverlayPref.setTitle(R.string.preferences_show_stroke_overlay);
        strokeOverlayPref.setSummaryOn(R.string.preferences_show_stroke_overlay_on);
        strokeOverlayPref.setSummaryOff(R.string.preferences_show_stroke_overlay_off);
        strokeOverlayPref.setDefaultValue(true);
        drawingCategory.addPreference(strokeOverlayPref);

        // Stroke fade duration preference
        SeekBarPreference strokeFadePref = new SeekBarPreference(getContext());
        strokeFadePref.setKey(Constants.PreferenceKeys.KEY_STROKE_FADE_DURATION);
        strokeFadePref.setTitle(R.string.preferences_stroke_fade_speed);
        strokeFadePref.setMin(300);
        strokeFadePref.setMax(4000);
        strokeFadePref.setDefaultValue(1200);
        strokeFadePref.setShowSeekBarValue(true);
        strokeFadePref.setUpdatesContinuously(true);
        addSeekBarWithReset(drawingCategory, strokeFadePref, 1200);

        // Display preferences category
        PreferenceCategory displayCategory = new PreferenceCategory(getContext());
        displayCategory.setTitle(R.string.preferences_display_category);
        screen.addPreference(displayCategory);

        // App theme preference
        DropDownPreference themePref = new DropDownPreference(getContext());
        themePref.setKey(Constants.PreferenceKeys.KEY_APP_THEME);
        themePref.setTitle(R.string.preferences_canvas_theme);

        // Set entries and values
        CharSequence[] entries = {
            getString(R.string.preferences_canvas_theme_system),
            getString(R.string.preferences_canvas_theme_light),
            getString(R.string.preferences_canvas_theme_dark)
        };
        CharSequence[] values = {"system", "light", "dark"};

        themePref.setEntries(entries);
        themePref.setEntryValues(values);
        themePref.setDefaultValue("system");
        displayCategory.addPreference(themePref);

        // Grid visible preference
        CheckBoxPreference gridVisiblePref = new CheckBoxPreference(getContext());
        gridVisiblePref.setKey(Constants.PreferenceKeys.KEY_GRID_VISIBLE);
        gridVisiblePref.setTitle(R.string.preferences_grid_visible);
        gridVisiblePref.setSummaryOn(R.string.preferences_grid_visible_on);
        gridVisiblePref.setSummaryOff(R.string.preferences_grid_visible_off);
        gridVisiblePref.setDefaultValue(true);
        displayCategory.addPreference(gridVisiblePref);

        // Grid opacity preference
        SeekBarPreference gridOpacityPref = new SeekBarPreference(getContext());
        gridOpacityPref.setKey(Constants.PreferenceKeys.KEY_GRID_OPACITY);
        gridOpacityPref.setTitle(R.string.preferences_grid_opacity);
        gridOpacityPref.setMin(0);
        gridOpacityPref.setMax(100);
        gridOpacityPref.setDefaultValue(30);
        gridOpacityPref.setShowSeekBarValue(true);
        addSeekBarWithReset(displayCategory, gridOpacityPref, 30);

        // Template opacity preference
        SeekBarPreference templateOpacityPref = new SeekBarPreference(getContext());
        templateOpacityPref.setKey(Constants.PreferenceKeys.KEY_TEMPLATE_OPACITY);
        templateOpacityPref.setTitle(R.string.preferences_template_opacity);
        templateOpacityPref.setMin(0);
        templateOpacityPref.setMax(100);
        templateOpacityPref.setDefaultValue(50);
        templateOpacityPref.setShowSeekBarValue(true);
        addSeekBarWithReset(displayCategory, templateOpacityPref, 50);

        // Template scale mode preference
        DropDownPreference templateScalePref = new DropDownPreference(getContext());
        templateScalePref.setKey(Constants.PreferenceKeys.KEY_TEMPLATE_SCALE_MODE);
        templateScalePref.setTitle(R.string.preferences_template_scale_mode);

        // Set entries and values for scale modes
        CharSequence[] scaleEntries = {
            getString(R.string.preferences_template_scale_fit_center),
            getString(R.string.preferences_template_scale_fit_xy),
            getString(R.string.preferences_template_scale_center_crop),
            getString(R.string.preferences_template_scale_center_inside)
        };
        CharSequence[] scaleValues = {
            Constants.TemplateScaleModes.FIT_CENTER,
            Constants.TemplateScaleModes.FIT_XY,
            Constants.TemplateScaleModes.CENTER_CROP,
            Constants.TemplateScaleModes.CENTER_INSIDE
        };

        templateScalePref.setEntries(scaleEntries);
        templateScalePref.setEntryValues(scaleValues);
        templateScalePref.setDefaultValue(Constants.TemplateScaleModes.FIT_CENTER);
        displayCategory.addPreference(templateScalePref);

        // Set the preference screen
        setPreferenceScreen(screen);

        // Wire dependencies after screen is attached so lookup succeeds
        strokeFadePref.setDependency(Constants.PreferenceKeys.KEY_STROKE_OVERLAY_VISIBLE);
    }

    /**
     * Adds a seek bar preference with a reset option to the given category.
     * @param category The preference category to add to.
     * @param seekBar The seek bar preference to add.
     * @param defaultValue The default value for reset.
     */
    private void addSeekBarWithReset(PreferenceCategory category, SeekBarPreference seekBar, int defaultValue) {
        category.addPreference(seekBar);
        Preference resetPref = new Preference(getContext());
        resetPref.setTitle(getString(R.string.preferences_reset, seekBar.getTitle()));
        resetPref.setOnPreferenceClickListener(preference -> {
            seekBar.setValue(defaultValue);
            return true;
        });
        category.addPreference(resetPref);
    }
}
