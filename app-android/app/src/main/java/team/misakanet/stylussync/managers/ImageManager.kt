package team.misakanet.stylussync.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.InputStream
import team.misakanet.stylussync.utils.Constants

/**
 * Manages loading and processing of template images for the canvas.
 */
class ImageManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageManager"
    }
    
    /**
     * Loads a bitmap from the given URI.
     * @param imageUri The URI of the image to load.
     * @return The loaded bitmap, or null if loading fails.
     */
    fun loadBitmap(imageUri: Uri?): Bitmap? = runCatching {
        imageUri?.let { uri ->
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        }
    }.onFailure { e ->
        Log.e(TAG, "Error loading bitmap", e)
    }.getOrNull()
    
    /**
     * Loads a bitmap from the given URI with efficient memory usage by scaling it down.
     * @param imageUri The URI of the image to load.
     * @param reqWidth The required width for the scaled bitmap.
     * @param reqHeight The required height for the scaled bitmap.
     * @return The loaded and scaled bitmap, or null if loading fails.
     */
    fun loadBitmapScaled(imageUri: Uri?, reqWidth: Int, reqHeight: Int): Bitmap? = runCatching {
        imageUri?.let { uri ->
            // First decode to get dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            
            // Calculate sample size
            if (reqWidth > 0 && reqHeight > 0) {
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            }
            
            // Decode with sample size
            options.inJustDecodeBounds = false
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        }
    }.onFailure { e ->
        Log.e(TAG, "Error loading scaled bitmap", e)
    }.getOrNull()
    
    /**
     * Calculates the optimal sample size for bitmap decoding to optimize memory usage.
     * @param options The BitmapFactory options containing image dimensions.
     * @param reqWidth The required width.
     * @param reqHeight The required height.
     * @return The calculated sample size.
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = Constants.ImageConfig.BITMAP_SAMPLE_SIZE_MIN

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
