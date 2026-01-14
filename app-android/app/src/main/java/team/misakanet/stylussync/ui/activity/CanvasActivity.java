package team.misakanet.stylussync.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

import team.misakanet.stylussync.R;
import team.misakanet.stylussync.data.preferences.PreferenceManager;
import team.misakanet.stylussync.listener.MidiConnectionListener;
import team.misakanet.stylussync.managers.ImageManager;
import team.misakanet.stylussync.managers.PermissionManager;
import team.misakanet.stylussync.midi.MidiClient;
import team.misakanet.stylussync.ui.view.CanvasView;
import team.misakanet.stylussync.utils.Constants;
import team.misakanet.stylussync.utils.ThemeHelper;

/**
 * Main activity for the canvas interface, handling MIDI connections, image templates, and user interactions.
 */
public class CanvasActivity extends AppCompatActivity implements View.OnSystemUiVisibilityChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "CanvasActivity";

    /** MIDI client for handling MIDI communications. */
    private MidiClient midiClient;
    /** Manager for application preferences. */
    private PreferenceManager preferenceManager;
    /** Manager for runtime permissions. */
    private PermissionManager permissionManager;
    /** Manager for image loading and processing. */
    private ImageManager imageManager;
    
    /** Flag indicating if the activity is in full screen mode. */
    private boolean fullScreen = false;
    /** Launcher for image picker activity. */
    private ActivityResultLauncher<Intent> imagePickerLauncher;


    /**
     * Called when the activity is starting.
     * @param savedInstanceState If the activity is being re-created from a previous saved state, this is the state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize managers
        preferenceManager = new PreferenceManager(this);
        permissionManager = new PermissionManager(this);
        imageManager = new ImageManager(this);
        
        // Apply theme
        ThemeHelper.applyTheme(preferenceManager.getSharedPreferences());
        super.onCreate(savedInstanceState);

        // Initialize image picker
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        handleSelectedImage(selectedImage);
                    }
                }
            }
        );

        // Handle back button: exit full-screen first, then exit Activity
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (fullScreen) {
                    switchFullScreen(null);
                } else {
                    setEnabled(false);
                    finish();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);

        setContentView(R.layout.activity_canvas);

        // Create and initialize MIDI client
        midiClient = new MidiClient(this);
        midiClient.setStatusListener(new MidiConnectionListener() {
            @Override
            public void onMidiConnected(String deviceName) {
                Log.i(TAG, "MIDI device connected: " + deviceName);
                Toast.makeText(CanvasActivity.this, 
                    "MIDI device connected: " + deviceName, 
                    Toast.LENGTH_LONG).show();
                updateUIForMidiStatus(true);
            }

            @Override
            public void onMidiDisconnected() {
                Log.i(TAG, "MIDI device disconnected");
                Toast.makeText(CanvasActivity.this, 
                    "MIDI device disconnected, please connect USB or Bluetooth MIDI device", 
                    Toast.LENGTH_LONG).show();
                updateUIForMidiStatus(false);
            }

            @Override
            public void onMidiError(String error) {
                Log.e(TAG, "MIDI error: " + error);
                Toast.makeText(CanvasActivity.this, 
                    "MIDI error: " + error, 
                    Toast.LENGTH_LONG).show();
                updateUIForMidiStatus(false);
            }
        });

        // Initialize MIDI
        if (midiClient.initializeMidi()) {
            midiClient.start();
            Log.i(TAG, "MIDI client started");
        } else {
            Toast.makeText(this, "MIDI initialization failed", Toast.LENGTH_LONG).show();
        }

        // Set up CanvasView
        CanvasView canvas = findViewById(R.id.canvas);
        canvas.setMidiClient(midiClient);

        // Register preference change listener
        preferenceManager.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (preferenceManager.shouldKeepDisplayActive())
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        showTemplateImage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (midiClient != null) {
            midiClient.stop();
        }
        // Unregister preference change listener
        preferenceManager.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
    
    /**
     * Update UI to reflect MIDI connection status
     */
    private void updateUIForMidiStatus(boolean connected) {
        TextView messageView = findViewById(R.id.canvas_message);
        
        if (connected) {
            findViewById(R.id.canvas_template).setVisibility(View.VISIBLE);
            findViewById(R.id.canvas).setVisibility(View.VISIBLE);
            messageView.setVisibility(View.GONE);
            showTemplateImage();
        } else {
            findViewById(R.id.canvas_template).setVisibility(View.GONE);
            findViewById(R.id.canvas).setVisibility(View.GONE);
            messageView.setVisibility(View.VISIBLE);
            messageView.setText("Waiting for MIDI device...\n\nPlease connect to computer via USB or Bluetooth");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_canvas, menu);
        return true;
    }

    public void showSettings(MenuItem item) {
        startActivityForResult(new Intent(this, SettingsActivity.class), 0);
    }


    // full-screen methods

    public void switchFullScreen(MenuItem item) {
        final View decorView = getWindow().getDecorView();
        int uiFlags = decorView.getSystemUiVisibility();

        uiFlags ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        uiFlags ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        uiFlags ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setOnSystemUiVisibilityChangeListener(this);
        decorView.setSystemUiVisibility(uiFlags);
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        Log.i("StylusSync", "System UI changed " + visibility);

        fullScreen = (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0;

        // show/hide action bar according to full-screen mode
        if (fullScreen) {
            Objects.requireNonNull(CanvasActivity.this.getSupportActionBar()).hide();
            Toast.makeText(CanvasActivity.this, "Press Back button to leave full-screen mode.", Toast.LENGTH_LONG).show();
        } else
            Objects.requireNonNull(CanvasActivity.this.getSupportActionBar()).show();
    }


    // template image logic

    public void setTemplateImage(MenuItem item) {
        if (preferenceManager.getTemplateImageUri() == null)
            selectTemplateImage(item);
        else {
            // template image already set, show popup
            PopupMenu popup = new PopupMenu(this, findViewById(R.id.menu_set_template_image));
            popup.getMenuInflater().inflate(R.menu.set_template_image, popup.getMenu());
            popup.show();
        }
    }

    public void selectTemplateImage(MenuItem item) {
        // Check and request permissions
        if (!permissionManager.hasStoragePermission()) {
            permissionManager.requestStoragePermission();
            return;
        }
        
        // Launch image picker
        launchImagePicker();
    }
    
    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionManager.isPermissionGranted(requestCode, grantResults)) {
            // Permission granted, launch image picker
            launchImagePicker();
        } else {
            Toast.makeText(this, "Storage permission required to select template image", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Handle selected image
     */
    private void handleSelectedImage(Uri imageUri) {
        try {
            // Load bitmap using ImageManager
            Bitmap bitmap = imageManager.loadBitmap(imageUri);
            
            if (bitmap != null) {
                // Save URI instead of file path
                preferenceManager.setTemplateImageUri(imageUri);
                showTemplateImage();
                Toast.makeText(this, "Template image set", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cannot load image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(this, "Image loading failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void clearTemplateImage(MenuItem item) {
        preferenceManager.clearTemplateImage();
        showTemplateImage();
        Toast.makeText(this, "Template image cleared", Toast.LENGTH_SHORT).show();
    }

    public void showTemplateImage() {
        ImageView template = findViewById(R.id.canvas_template);

        // Apply template opacity
        float templateOpacity = preferenceManager.getSharedPreferences().getInt(Constants.PreferenceKeys.KEY_TEMPLATE_OPACITY, 50) / 100.0f;
        template.setAlpha(templateOpacity);

        // Apply template scale mode
        String scaleMode = preferenceManager.getSharedPreferences().getString(Constants.PreferenceKeys.KEY_TEMPLATE_SCALE_MODE, Constants.TemplateScaleModes.FIT_CENTER);
        ImageView.ScaleType scaleType;
        switch (scaleMode) {
            case Constants.TemplateScaleModes.FIT_XY:
                scaleType = ImageView.ScaleType.FIT_XY;
                break;
            case Constants.TemplateScaleModes.CENTER_CROP:
                scaleType = ImageView.ScaleType.CENTER_CROP;
                break;
            case Constants.TemplateScaleModes.CENTER_INSIDE:
                scaleType = ImageView.ScaleType.CENTER_INSIDE;
                break;
            case Constants.TemplateScaleModes.FIT_CENTER:
            default:
                scaleType = ImageView.ScaleType.FIT_CENTER;
                break;
        }
        template.setScaleType(scaleType);

        template.setImageDrawable(null);

        if (template.getVisibility() == View.VISIBLE) {
            String imageUriString = preferenceManager.getTemplateImageUri();
            if (imageUriString != null) {
                try {
                    Uri imageUri = Uri.parse(imageUriString);

                    // Load bitmap using ImageManager with efficient memory usage
                    int width = template.getWidth();
                    int height = template.getHeight();

                    Bitmap bitmap;
                    if (width > 0 && height > 0) {
                        bitmap = imageManager.loadBitmapScaled(imageUri, width, height);
                    } else {
                        bitmap = imageManager.loadBitmap(imageUri);
                    }

                    if (bitmap != null) {
                        template.setImageBitmap(bitmap);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error displaying template image", e);
                    Toast.makeText(this, getString(R.string.cannot_display_template_image), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case Constants.PreferenceKeys.KEY_TEMPLATE_OPACITY:
            case Constants.PreferenceKeys.KEY_TEMPLATE_SCALE_MODE:
                showTemplateImage();
                break;
        }
    }
}
