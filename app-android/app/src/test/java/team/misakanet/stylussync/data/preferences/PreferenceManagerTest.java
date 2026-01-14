package team.misakanet.stylussync.data.preferences;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

/**
 * Unit tests for PreferenceManager class using Robolectric.
 */
@RunWith(RobolectricTestRunner.class)
public class PreferenceManagerTest {

    private Context context;
    private PreferenceManager preferenceManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        preferenceManager = new PreferenceManager(context);
    }

    @After
    public void tearDown() {
        // Clear preferences after each test
        preferenceManager.getSharedPreferences().edit().clear().apply();
    }

    @Test
    public void testSetAndGetTemplateImageUri() {
        String testUri = "content://test/image";
        preferenceManager.setTemplateImageUri(android.net.Uri.parse(testUri));

        String retrievedUri = preferenceManager.getTemplateImageUri();
        assertEquals(testUri, retrievedUri);
    }

    @Test
    public void testClearTemplateImage() {
        String testUri = "content://test/image";
        preferenceManager.setTemplateImageUri(android.net.Uri.parse(testUri));
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