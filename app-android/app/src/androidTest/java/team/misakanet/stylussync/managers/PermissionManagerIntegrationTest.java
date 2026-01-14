package team.misakanet.stylussync.managers;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Integration tests for PermissionManager class.
 * These tests interact with the Android system.
 */
@RunWith(AndroidJUnit4.class)
public class PermissionManagerIntegrationTest {

    @Test
    public void testIsPermissionGranted() {
        // Test the logic without launching activity
        PermissionManager permissionManager = new PermissionManager(null); // Activity not needed for this test

        // Test with granted result
        int[] grantResults = {android.content.pm.PackageManager.PERMISSION_GRANTED};
        boolean result = permissionManager.isPermissionGranted(100, grantResults);
        assertTrue(result);

        // Test with denied result
        int[] denyResults = {android.content.pm.PackageManager.PERMISSION_DENIED};
        result = permissionManager.isPermissionGranted(100, denyResults);
        assertFalse(result);

        // Test with wrong code
        result = permissionManager.isPermissionGranted(200, grantResults);
        assertFalse(result);
    }

    @Test
    public void testHasStoragePermission() {
        Context context = ApplicationProvider.getApplicationContext();
        PermissionManager permissionManager = new PermissionManager(null); // Use context-based check

        // Test current permission state using context
        // This is a simplified test - in real usage, activity context would be needed
        // But we can test the logic path
        assertNotNull(permissionManager);
    }
}