package team.misakanet.stylussync.managers;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import team.misakanet.stylussync.utils.Constants;

/**
 * Manages runtime permissions required by the application, particularly storage permissions.
 */
public class PermissionManager {
    
    /** The activity context for permission operations. */
    private final Activity activity;
    
    /**
     * Constructs a PermissionManager with the given activity.
     * @param activity The activity to use for permission requests.
     */
    public PermissionManager(Activity activity) {
        this.activity = activity;
    }
    
    /**
     * Checks if storage permission is granted, handling different Android versions.
     * @return true if storage permission is granted, false otherwise.
     */
    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES
            return ContextCompat.checkSelfPermission(activity, 
                Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 12 and below use READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(activity, 
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    /**
     * Requests storage permission based on the Android version.
     */
    public void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                Constants.RequestCodes.PERMISSION_READ_STORAGE);
        } else {
            ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                Constants.RequestCodes.PERMISSION_READ_STORAGE);
        }
    }
    
    /**
     * Checks if the permission request result indicates that permission was granted.
     * @param requestCode The request code from the permission request.
     * @param grantResults The grant results array.
     * @return true if the permission was granted, false otherwise.
     */
    public boolean isPermissionGranted(int requestCode, int[] grantResults) {
        return requestCode == Constants.RequestCodes.PERMISSION_READ_STORAGE 
            && grantResults.length > 0 
            && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }
}
