package team.misakanet.stylussync.data.preferences

import android.content.Context
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.*

/**
 * Unit tests for PreferenceManager class using Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class PreferenceManagerTest {

    private lateinit var context: Context
    private lateinit var preferenceManager: PreferenceManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        preferenceManager = PreferenceManager(context)
    }

    @After
    fun tearDown() {
        // Clear preferences after each test
        preferenceManager.sharedPreferences.edit().clear().apply()
    }

    @Test
    fun testSetAndGetTemplateImageUri() {
        val testUri = "content://test/image"
        preferenceManager.setTemplateImageUri(android.net.Uri.parse(testUri))

        val retrievedUri = preferenceManager.getTemplateImageUri()
        assertEquals("Retrieved URI should match the set URI", testUri, retrievedUri)
    }

    @Test
    fun testClearTemplateImage() {
        val testUri = "content://test/image"
        preferenceManager.setTemplateImageUri(android.net.Uri.parse(testUri))
        preferenceManager.clearTemplateImage()

        val retrievedUri = preferenceManager.getTemplateImageUri()
        assertNull("Template image URI should be null after clearing", retrievedUri)
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
