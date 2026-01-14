package team.misakanet.stylussync.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;

import team.misakanet.stylussync.utils.Constants;

/**
 * Manages loading and processing of template images for the canvas.
 */
public class ImageManager {
    
    private static final String TAG = "ImageManager";
    
    /** The application context. */
    private final Context context;
    
    /**
     * Constructs an ImageManager with the given context.
     * @param context The application context.
     */
    public ImageManager(Context context) {
        this.context = context;
    }
    
    /**
     * Loads a bitmap from the given URI.
     * @param imageUri The URI of the image to load.
     * @return The loaded bitmap, or null if loading fails.
     */
    public Bitmap loadBitmap(Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                return bitmap;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading bitmap", e);
        }
        return null;
    }
    
    /**
     * Loads a bitmap from the given URI with efficient memory usage by scaling it down.
     * @param imageUri The URI of the image to load.
     * @param reqWidth The required width for the scaled bitmap.
     * @param reqHeight The required height for the scaled bitmap.
     * @return The loaded and scaled bitmap, or null if loading fails.
     */
    public Bitmap loadBitmapScaled(Uri imageUri, int reqWidth, int reqHeight) {
        try {
            // First decode to get dimensions
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return null;
            }
            
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            // Calculate sample size
            if (reqWidth > 0 && reqHeight > 0) {
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            }
            
            // Decode with sample size
            options.inJustDecodeBounds = false;
            inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error loading scaled bitmap", e);
            return null;
        }
    }
    
    /**
     * Calculates the optimal sample size for bitmap decoding to optimize memory usage.
     * @param options The BitmapFactory options containing image dimensions.
     * @param reqWidth The required width.
     * @param reqHeight The required height.
     * @return The calculated sample size.
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = Constants.ImageConfig.BITMAP_SAMPLE_SIZE_MIN;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
