package team.misakanet.stylussync.ui.activity

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import team.misakanet.stylussync.R
import team.misakanet.stylussync.data.preferences.PreferenceManager
import team.misakanet.stylussync.listener.MidiConnectionListener
import team.misakanet.stylussync.managers.ImageManager
import team.misakanet.stylussync.managers.PermissionManager
import team.misakanet.stylussync.midi.MidiClient
import team.misakanet.stylussync.ui.view.CanvasView
import team.misakanet.stylussync.utils.Constants
import team.misakanet.stylussync.utils.ThemeHelper

/**
 * Main activity for the canvas interface, handling MIDI connections, image templates, and user interactions.
 */
class CanvasActivity : AppCompatActivity(), View.OnSystemUiVisibilityChangeListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    
    companion object {
        private const val TAG = "CanvasActivity"
    }
    
    /** MIDI client for handling MIDI communications. */
    private lateinit var midiClient: MidiClient
    /** Manager for application preferences. */
    private lateinit var preferenceManager: PreferenceManager
    /** Manager for runtime permissions. */
    private lateinit var permissionManager: PermissionManager
    /** Manager for image loading and processing. */
    private lateinit var imageManager: ImageManager
    
    /** Flag indicating if the activity is in full screen mode. */
    private var fullScreen = false
    /** Launcher for image picker activity. */
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    
    /**
     * Called when the activity is starting.
     * @param savedInstanceState If the activity is being re-created from a previous saved state, this is the state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize managers
        preferenceManager = PreferenceManager(this)
        permissionManager = PermissionManager(this)
        imageManager = ImageManager(this)
        
        // Apply theme
        ThemeHelper.applyTheme(preferenceManager.sharedPreferences)
        super.onCreate(savedInstanceState)
        
        // Initialize image picker
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val selectedImage = result.data?.data
                if (selectedImage != null) {
                    handleSelectedImage(selectedImage)
                }
            }
        }
        
        // Handle back button: exit full-screen first, then exit Activity
        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fullScreen) {
                    switchFullScreen(null)
                } else {
                    isEnabled = false
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        
        setContentView(R.layout.activity_canvas)
        
        // Create and initialize MIDI client
        midiClient = MidiClient(this)
        midiClient.setStatusListener(object : MidiConnectionListener {
            override fun onMidiConnected(deviceName: String) {
                Log.i(TAG, "MIDI device connected: $deviceName")
                Toast.makeText(
                    this@CanvasActivity,
                    "MIDI device connected: $deviceName",
                    Toast.LENGTH_LONG
                ).show()
                updateUIForMidiStatus(true)
            }
            
            override fun onMidiDisconnected() {
                Log.i(TAG, "MIDI device disconnected")
                Toast.makeText(
                    this@CanvasActivity,
                    "MIDI device disconnected, please connect USB or Bluetooth MIDI device",
                    Toast.LENGTH_LONG
                ).show()
                updateUIForMidiStatus(false)
            }
            
            override fun onMidiError(error: String) {
                Log.e(TAG, "MIDI error: $error")
                Toast.makeText(
                    this@CanvasActivity,
                    "MIDI error: $error",
                    Toast.LENGTH_LONG
                ).show()
                updateUIForMidiStatus(false)
            }
        })
        
        // Initialize MIDI
        if (midiClient.initializeMidi()) {
            midiClient.start()
            Log.i(TAG, "MIDI client started")
        } else {
            Toast.makeText(this, "MIDI initialization failed", Toast.LENGTH_LONG).show()
        }
        
        // Set up CanvasView
        val canvas = findViewById<CanvasView>(R.id.canvas)
        canvas.midiClient = midiClient
        
        // Register preference change listener
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }
    
    override fun onResume() {
        super.onResume()
        
        window.apply {
            if (preferenceManager.shouldKeepDisplayActive()) {
                addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        
        showTemplateImage()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        midiClient.stop()
        // Unregister preference change listener
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
    
    /**
     * Update UI to reflect MIDI connection status
     */
    private fun updateUIForMidiStatus(connected: Boolean) {
        findViewById<TextView>(R.id.canvas_message).apply {
            visibility = if (connected) View.GONE else View.VISIBLE
            if (!connected) {
                text = "Waiting for MIDI device...\n\nPlease connect to computer via USB or Bluetooth"
            }
        }
        
        val templateView = findViewById<View>(R.id.canvas_template)
        val canvasView = findViewById<View>(R.id.canvas)
        
        templateView.visibility = if (connected) View.VISIBLE else View.GONE
        canvasView.visibility = if (connected) View.VISIBLE else View.GONE
        
        if (connected) {
            showTemplateImage()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_canvas, menu)
        return true
    }
    
    fun showSettings(item: MenuItem) {
        startActivityForResult(Intent(this, SettingsActivity::class.java), 0)
    }
    
    // full-screen methods
    
    fun switchFullScreen(item: MenuItem?) {
        val decorView = window.decorView
        var uiFlags = decorView.systemUiVisibility
        
        uiFlags = uiFlags xor View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        uiFlags = uiFlags xor View.SYSTEM_UI_FLAG_FULLSCREEN
        uiFlags = uiFlags xor View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        
        decorView.setOnSystemUiVisibilityChangeListener(this)
        decorView.systemUiVisibility = uiFlags
    }
    
    override fun onSystemUiVisibilityChange(visibility: Int) {
        Log.i("StylusSync", "System UI changed $visibility")
        
        fullScreen = (visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
        
        // show/hide action bar according to full-screen mode
        if (fullScreen) {
            supportActionBar?.hide()
            Toast.makeText(
                this@CanvasActivity,
                "Press Back button to leave full-screen mode.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            supportActionBar?.show()
        }
    }
    
    // template image logic
    
    fun setTemplateImage(item: MenuItem) {
        if (preferenceManager.getTemplateImageUri() == null) {
            selectTemplateImage(item)
        } else {
            // template image already set, show popup
            PopupMenu(this, findViewById(R.id.menu_set_template_image)).apply {
                menuInflater.inflate(R.menu.set_template_image, menu)
                show()
            }
        }
    }
    
    fun selectTemplateImage(item: MenuItem) {
        // Check and request permissions
        if (!permissionManager.hasStoragePermission()) {
            permissionManager.requestStoragePermission()
            return
        }
        
        // Launch image picker
        launchImagePicker()
    }
    
    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionManager.isPermissionGranted(requestCode, grantResults)) {
            // Permission granted, launch image picker
            launchImagePicker()
        } else {
            Toast.makeText(
                this,
                "Storage permission required to select template image",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Handle selected image
     */
    private fun handleSelectedImage(imageUri: Uri) {
        try {
            // Load bitmap using ImageManager
            val bitmap = imageManager.loadBitmap(imageUri)
            
            if (bitmap != null) {
                // Save URI instead of file path
                preferenceManager.setTemplateImageUri(imageUri)
                showTemplateImage()
                Toast.makeText(this, "Template image set", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cannot load image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
            Toast.makeText(
                this,
                "Image loading failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    fun clearTemplateImage(item: MenuItem) {
        preferenceManager.clearTemplateImage()
        showTemplateImage()
        Toast.makeText(this, "Template image cleared", Toast.LENGTH_SHORT).show()
    }
    
    fun showTemplateImage() {
        val template = findViewById<ImageView>(R.id.canvas_template)
        
        // Apply template opacity
        val templateOpacity = preferenceManager.sharedPreferences.getInt(
            Constants.PreferenceKeys.KEY_TEMPLATE_OPACITY,
            50
        ) / 100.0f
        template.alpha = templateOpacity
        
        // Apply template scale mode
        val scaleMode = preferenceManager.sharedPreferences.getString(
            Constants.PreferenceKeys.KEY_TEMPLATE_SCALE_MODE,
            Constants.TemplateScaleModes.FIT_CENTER
        ) ?: Constants.TemplateScaleModes.FIT_CENTER
        
        val scaleType = when (scaleMode) {
            Constants.TemplateScaleModes.FIT_XY -> ImageView.ScaleType.FIT_XY
            Constants.TemplateScaleModes.CENTER_CROP -> ImageView.ScaleType.CENTER_CROP
            Constants.TemplateScaleModes.CENTER_INSIDE -> ImageView.ScaleType.CENTER_INSIDE
            else -> ImageView.ScaleType.FIT_CENTER
        }
        template.scaleType = scaleType
        
        template.setImageDrawable(null)
        
        if (template.visibility == View.VISIBLE) {
            val imageUriString = preferenceManager.getTemplateImageUri()
            if (imageUriString != null) {
                try {
                    val imageUri = Uri.parse(imageUriString)
                    
                    // Load bitmap using ImageManager with efficient memory usage
                    val width = template.width
                    val height = template.height
                    
                    val bitmap = if (width > 0 && height > 0) {
                        imageManager.loadBitmapScaled(imageUri, width, height)
                    } else {
                        imageManager.loadBitmap(imageUri)
                    }
                    
                    if (bitmap != null) {
                        template.setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error displaying template image", e)
                    Toast.makeText(
                        this,
                        getString(R.string.cannot_display_template_image),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Constants.PreferenceKeys.KEY_TEMPLATE_OPACITY,
            Constants.PreferenceKeys.KEY_TEMPLATE_SCALE_MODE -> {
                showTemplateImage()
            }
        }
    }
}
