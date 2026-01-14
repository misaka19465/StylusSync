package team.misakanet.stylussync.data.preferences;

import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Integration tests for PreferenceManager class.
 * These tests interact with actual SharedPreferences.
 */
@RunWith(AndroidJUnit4.class)
public class PreferenceManagerIntegrationTest {

    private Context context;
    private PreferenceManager preferenceManager;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        preferenceManager = new PreferenceManager(context);
    }

    @After
    public void tearDown() {
        // Clear preferences after each test
        preferenceManager.getSharedPreferences().edit().clear().apply();
    }

    @Test
    public void testSetAndGetTemplateImageUri() {
        Uri testUri = Uri.parse("content://test/image");
        preferenceManager.setTemplateImageUri(testUri);

        String retrievedUri = preferenceManager.getTemplateImageUri();
        assertEquals(testUri.toString(), retrievedUri);
    }

    @Test
    public void testClearTemplateImage() {
        Uri testUri = Uri.parse("content://test/image");
        preferenceManager.setTemplateImageUri(testUri);
        preferenceManager.clearTemplateImage();

        String retrievedUri = preferenceManager.getTemplateImageUri();
        assertNull(retrievedUri);
    }

    @Test
    public void testShouldKeepDisplayActive_Default() {
        boolean result = preferenceManager.shouldKeepDisplayActive();
        assertTrue(result); // Default is true
    }

    @Test
    public void testIsStylusOnly_Default() {
        boolean result = preferenceManager.isStylusOnly();
        assertFalse(result); // Default is false
    }

    @Test
    public void testGetAppTheme_Default() {
        String theme = preferenceManager.getAppTheme();
        assertEquals(team.misakanet.stylussync.utils.Constants.ThemeValues.SYSTEM, theme);
    }
}