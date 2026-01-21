package team.misakanet.stylussync.managers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import team.misakanet.stylussync.utils.Constants

/**
 * Manages runtime permissions required by the application, particularly storage permissions.
 */
class PermissionManager(private val activity: Activity) {
    
    companion object {
        private fun getStoragePermission(): String = 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
    }
    
    /**
     * Checks if storage permission is granted, handling different Android versions.
     * @return true if storage permission is granted, false otherwise.
     */
    fun hasStoragePermission(): Boolean =
        ContextCompat.checkSelfPermission(activity, getStoragePermission()) == 
            PackageManager.PERMISSION_GRANTED
    
    /**
     * Requests storage permission based on the Android version.
     */
    fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(getStoragePermission()),
            Constants.RequestCodes.PERMISSION_READ_STORAGE
        )
    }
    
    /**
     * Checks if the permission request result indicates that permission was granted.
     * @param requestCode The request code from the permission request.
     * @param grantResults The grant results array.
     * @return true if the permission was granted, false otherwise.
     */
    fun isPermissionGranted(requestCode: Int, grantResults: IntArray): Boolean =
        requestCode == Constants.RequestCodes.PERMISSION_READ_STORAGE &&
        grantResults.isNotEmpty() &&
        grantResults[0] == PackageManager.PERMISSION_GRANTED
}
