package team.misakanet.stylussync.managers

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.*

/**
 * Unit tests for ImageManager class using Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class ImageManagerTest {

    private lateinit var context: Context
    private lateinit var imageManager: ImageManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        imageManager = ImageManager(context)
    }

    @Test
    fun testLoadBitmap_NullUri() {
        // Test with null URI
        val result = imageManager.loadBitmap(null)
        assertNull("Loading null URI should return null", result)
    }

    @Test
    fun testLoadBitmapScaled_NullUri() {
        // Test with null URI
        val result = imageManager.loadBitmapScaled(null, 100, 100)
        assertNull(result)
    }

    @Test
    fun testLoadBitmap_InvalidUri() {
        // Test with invalid URI - just verify no exceptions
        val invalidUri = android.net.Uri.parse("invalid://uri")
        val result = imageManager.loadBitmap(invalidUri)
        // Result may vary, just ensure no exceptions
    }
}
