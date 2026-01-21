package team.misakanet.stylussync.ui.fragment

import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.SeekBarPreference
import team.misakanet.stylussync.R
import team.misakanet.stylussync.utils.Constants

/**
 * Fragment for displaying and managing application settings.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    /**
     * Called during onCreate to supply the preferences for this fragment.
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     * @param rootKey If non-null, this preference fragment should be rooted at the PreferenceScreen with this key.
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val screen: PreferenceScreen = preferenceManager.createPreferenceScreen(requireContext())

        // System preferences category
        val systemCategory = PreferenceCategory(requireContext()).apply {
            setTitle(R.string.preferences_category_system)
        }
        screen.addPreference(systemCategory)

        // Keep display active preference
        val keepDisplayActivePref = CheckBoxPreference(requireContext()).apply {
            key = Constants.PreferenceKeys.KEY_KEEP_DISPLAY_ACTIVE
            setTitle(R.string.preferences_keep_display_active)
            setSummaryOn(R.string.preferences_keep_display_active_on)
            setSummaryOff(R.string.preferences_keep_display_active_off)
            setDefaultValue(true)
        }
        systemCategory.addPreference(keepDisplayActivePref)

        // Drawing preferences category
        val drawingCategory = PreferenceCategory(requireContext()).apply {
            setTitle(R.string.preferences_category_drawing)
        }
        screen.addPreference(drawingCategory)

        // Stylus only preference
        val stylusOnlyPref = CheckBoxPreference(requireContext()).apply {
            key = Constants.PreferenceKeys.KEY_STYLUS_ONLY
            setTitle(R.string.preferences_stylus_only)
            setSummaryOn(R.string.preferences_stylus_only_on)
            setSummaryOff(R.string.preferences_stylus_only_off)
            setDefaultValue(false)
        }
        drawingCategory.addPreference(stylusOnlyPref)

        // Stroke overlay preference
        val strokeOverlayPref = CheckBoxPreference(requireContext()).apply {
            key = Constants.PreferenceKeys.KEY_STROKE_OVERLAY_VISIBLE
            setTitle(R.string.preferences_show_stroke_overlay)
            setSummaryOn(R.string.preferences_show_stroke_overlay_on)
            setSummaryOff(R.string.preferences_show_stroke_overlay_off)
            setDefaultValue(true)
        }
        drawingCategory.addPreference(strokeOverlayPref)

        // Stroke fade duration preference
        val strokeFadePref = SeekBarPreference(requireContext()).apply {
            key = Constants.PreferenceKeys.KEY_STROKE_FADE_DURATION
            setTitle(R.string.preferences_stroke_fade_speed)
            min = 300
            max = 4000
            setDefaultValue(1200)
            showSeekBarValue = true
            updatesContinuously = true
        }
        addSeekBarWithReset(drawingCategory, strokeFadePref, 1200)

        // Display preferences category
        val displayCategory = PreferenceCategory(requireContext()).apply {
            setTitle(R.string.preferences_display_category)
        }
        screen.addPreference(displayCategory)

        // App theme preference
        val themePref = DropDownPreference(requireContext()).apply {
            key = Constants.PreferenceKeys.KEY_APP_THEME
            setTitle(R.string.preferences_canvas_theme)

            // Set entries and values
            entries = arrayOf(
                getString(R.string.preferences_canvas_theme_system),
                getString(R.string.preferences_canvas_theme_light),
                getString(R.string.preferences_canvas_theme_dark)
            )
            entryValues = arrayOf("system", "light", "dark")
            setDefaultValue("system")
        }
        displayCategory.addPreference(themePref)

        // Grid visible preference
        val gridVisiblePref = CheckBoxPreference(requireContext()).apply {
            key = Constants.PreferenceKeys.KEY_GRID_VISIBLE
            setTitle(R.string.preferences_grid_visible)
            setSummaryOn(R.string.preferences_grid_visible_on)
            setSummaryOff(R.string.preferences_grid_visible_off)
            setDefaultValue(true)
        }
        displayCategory.addPreference(gridVisiblePref)

        // Grid opacity preference
        val gridOpacityPref = SeekBarPreference(requireContext()).apply {
            key = Constants.PreferenceKeys.KEY_GRID_OPACITY
            setTitle(R.string.preferences_grid_opacity)
            min = 0
            max = 100
            setDefaultValue(30)
            showSeekBarValue = true
        }
        addSeekBarWithReset(displayCategory, gridOpacityPref, 30)

        // Template opacity preference
        val templateOpacityPref = SeekBarPreference(requireContext()).apply {
            key = Constants.PreferenceKeys.KEY_TEMPLATE_OPACITY
            setTitle(R.string.preferences_template_opacity)
            min = 0
            max = 100
            setDefaultValue(50)
            showSeekBarValue = true
        }
        addSeekBarWithReset(displayCategory, templateOpacityPref, 50)

        // Template scale mode preference
        val templateScalePref = DropDownPreference(requireContext()).apply {
            key = Constants.PreferenceKeys.KEY_TEMPLATE_SCALE_MODE
            setTitle(R.string.preferences_template_scale_mode)

            // Set entries and values for scale modes
            entries = arrayOf(
                getString(R.string.preferences_template_scale_fit_center),
                getString(R.string.preferences_template_scale_fit_xy),
                getString(R.string.preferences_template_scale_center_crop),
                getString(R.string.preferences_template_scale_center_inside)
            )
            entryValues = arrayOf(
                Constants.TemplateScaleModes.FIT_CENTER,
                Constants.TemplateScaleModes.FIT_XY,
                Constants.TemplateScaleModes.CENTER_CROP,
                Constants.TemplateScaleModes.CENTER_INSIDE
            )
            setDefaultValue(Constants.TemplateScaleModes.FIT_CENTER)
        }
        displayCategory.addPreference(templateScalePref)

        // Set the preference screen
        preferenceScreen = screen

        // Wire dependencies after screen is attached so lookup succeeds
        strokeFadePref.dependency = Constants.PreferenceKeys.KEY_STROKE_OVERLAY_VISIBLE
    }

    /**
     * Adds a seek bar preference with a reset option to the given category.
     * @param category The preference category to add to.
     * @param seekBar The seek bar preference to add.
     * @param defaultValue The default value for reset.
     */
    private fun addSeekBarWithReset(
        category: PreferenceCategory,
        seekBar: SeekBarPreference,
        defaultValue: Int
    ) {
        category.addPreference(seekBar)
        val resetPref = Preference(requireContext()).apply {
            title = getString(R.string.preferences_reset, seekBar.title)
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                seekBar.value = defaultValue
                true
            }
        }
        category.addPreference(resetPref)
    }
}
