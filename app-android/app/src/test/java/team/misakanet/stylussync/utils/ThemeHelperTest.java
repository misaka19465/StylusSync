package team.misakanet.stylussync.utils;

import android.content.SharedPreferences;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

/**
 * Unit tests for ThemeHelper class.
 */
public class ThemeHelperTest {

    @Mock
    private SharedPreferences mockPrefs;

    public ThemeHelperTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testApplyTheme_System() {
        when(mockPrefs.getString(Constants.PreferenceKeys.KEY_APP_THEME, Constants.ThemeValues.SYSTEM))
            .thenReturn(Constants.ThemeValues.SYSTEM);

        ThemeHelper.applyTheme(mockPrefs);
        // Verify that AppCompatDelegate.setDefaultNightMode is called with MODE_NIGHT_FOLLOW_SYSTEM
        // In real test, use Robolectric to verify static calls
    }

    @Test
    public void testApplyTheme_Light() {
        ThemeHelper.applyTheme(Constants.ThemeValues.LIGHT);
        // Verify MODE_NIGHT_NO
    }

    @Test
    public void testApplyTheme_Dark() {
        ThemeHelper.applyTheme(Constants.ThemeValues.DARK);
        // Verify MODE_NIGHT_YES
    }

    @Test
    public void testApplyTheme_Default() {
        ThemeHelper.applyTheme("unknown");
        // Should default to system
    }
}