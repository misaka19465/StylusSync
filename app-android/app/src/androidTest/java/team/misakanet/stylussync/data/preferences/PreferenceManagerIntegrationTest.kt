package team.misakanet.stylussync.data.preferences

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration tests for PreferenceManager class.
 * These tests interact with actual SharedPreferences.
 */
@RunWith(AndroidJUnit4::class)
class PreferenceManagerIntegrationTest {

    private lateinit var context: Context
    private lateinit var preferenceManager: PreferenceManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferenceManager = PreferenceManager(context)
    }

    @After
    fun tearDown() {
        // Clear preferences after each test
        preferenceManager.sharedPreferences.edit().clear().apply()
    }

    @Test
    fun testSetAndGetTemplateImageUri() {
        val testUri = Uri.parse("content://test/image")
        preferenceManager.setTemplateImageUri(testUri)

        val retrievedUri = preferenceManager.getTemplateImageUri()
        assertEquals(testUri.toString(), retrievedUri)
    }

    @Test
    fun testClearTemplateImage() {
        val testUri = Uri.parse("content://test/image")
        preferenceManager.setTemplateImageUri(testUri)
        preferenceManager.clearTemplateImage()

        val retrievedUri = preferenceManager.getTemplateImageUri()
        assertNull(retrievedUri)
    }

    @Test
    fun testShouldKeepDisplayActive_Default() {
        val result = preferenceManager.shouldKeepDisplayActive()
        assertTrue(result) // Default is true
    }

    @Test
    fun testIsStylusOnly_Default() {
        val result = preferenceManager.isStylusOnly()
        assertFalse(result) // Default is false
    }

    @Test
    fun testGetAppTheme_Default() {
        val theme = preferenceManager.getAppTheme()
        assertEquals(team.misakanet.stylussync.utils.Constants.ThemeValues.SYSTEM, theme)
    }
}
