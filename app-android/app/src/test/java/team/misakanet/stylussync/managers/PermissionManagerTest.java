package team.misakanet.stylussync.managers;

import android.app.Activity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

/**
 * Unit tests for PermissionManager class using Robolectric.
 */
@RunWith(RobolectricTestRunner.class)
public class PermissionManagerTest {

    private Activity activity;
    private PermissionManager permissionManager;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        permissionManager = new PermissionManager(activity);
    }

    @Test
    public void testHasStoragePermission() {
        // Test with Robolectric - permission state depends on test setup
        boolean result = permissionManager.hasStoragePermission();
        // Just verify no exceptions are thrown
        assertNotNull(result);
    }

    @Test
    public void testRequestStoragePermission() {
        // Test that request doesn't throw exceptions
        permissionManager.requestStoragePermission();
        // Verify no exceptions
    }

    @Test
    public void testIsPermissionGranted_Granted() {
        int[] grantResults = {android.content.pm.PackageManager.PERMISSION_GRANTED};
        boolean result = permissionManager.isPermissionGranted(100, grantResults);
        assertTrue(result);
    }

    @Test
    public void testIsPermissionGranted_Denied() {
        int[] grantResults = {android.content.pm.PackageManager.PERMISSION_DENIED};
        boolean result = permissionManager.isPermissionGranted(100, grantResults);
        assertFalse(result);
    }

    @Test
    public void testIsPermissionGranted_WrongCode() {
        int[] grantResults = {android.content.pm.PackageManager.PERMISSION_GRANTED};
        boolean result = permissionManager.isPermissionGranted(200, grantResults);
        assertFalse(result);
    }
}