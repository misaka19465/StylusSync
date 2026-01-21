package team.misakanet.stylussync.managers

import android.app.Activity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.*

/**
 * Unit tests for PermissionManager class using Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class PermissionManagerTest {

    private lateinit var activity: Activity
    private lateinit var permissionManager: PermissionManager

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        permissionManager = PermissionManager(activity)
    }

    @Test
    fun testHasStoragePermission() {
        // Test with Robolectric - permission state depends on test setup
        val result = permissionManager.hasStoragePermission()
        // Just verify no exceptions are thrown
        assertNotNull("Permission check should return a valid boolean", result)
    }

    @Test
    fun testRequestStoragePermission() {
        // Test that request doesn't throw exceptions
        permissionManager.requestStoragePermission()
        // Verify no exceptions
    }

    @Test
    fun testIsPermissionGranted_Granted() {
        val grantResults = intArrayOf(android.content.pm.PackageManager.PERMISSION_GRANTED)
        val result = permissionManager.isPermissionGranted(100, grantResults)
        assertTrue(result)
    }

    @Test
    fun testIsPermissionGranted_Denied() {
        val grantResults = intArrayOf(android.content.pm.PackageManager.PERMISSION_DENIED)
        val result = permissionManager.isPermissionGranted(100, grantResults)
        assertFalse(result)
    }

    @Test
    fun testIsPermissionGranted_WrongCode() {
        val grantResults = intArrayOf(android.content.pm.PackageManager.PERMISSION_GRANTED)
        val result = permissionManager.isPermissionGranted(200, grantResults)
        assertFalse(result)
    }
}
