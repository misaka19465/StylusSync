package team.misakanet.stylussync.managers;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

/**
 * Unit tests for ImageManager class using Robolectric.
 */
@RunWith(RobolectricTestRunner.class)
public class ImageManagerTest {

    private Context context;
    private ImageManager imageManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        imageManager = new ImageManager(context);
    }

    @Test
    public void testLoadBitmap_NullUri() {
        // Test with null URI
        android.graphics.Bitmap result = imageManager.loadBitmap(null);
        assertNull(result);
    }

    @Test
    public void testLoadBitmapScaled_NullUri() {
        // Test with null URI
        android.graphics.Bitmap result = imageManager.loadBitmapScaled(null, 100, 100);
        assertNull(result);
    }

    @Test
    public void testLoadBitmap_InvalidUri() {
        // Test with invalid URI - just verify no exceptions
        android.net.Uri invalidUri = android.net.Uri.parse("invalid://uri");
        android.graphics.Bitmap result = imageManager.loadBitmap(invalidUri);
        // Result may vary, just ensure no exceptions
    }
}