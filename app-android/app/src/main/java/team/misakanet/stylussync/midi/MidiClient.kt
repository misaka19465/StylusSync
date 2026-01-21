package team.misakanet.stylussync.midi

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import team.misakanet.stylussync.listener.MidiConnectionListener

/**
 * Handles MIDI communication for sending touch events to a connected computer.
 */
class MidiClient(private val context: Context) : Runnable {
    
    companion object {
        private const val TAG = "MidiClient"
    }
    
    /** Queue for MIDI events to be sent. */
    val queue: LinkedBlockingQueue<MidiEvent> = LinkedBlockingQueue()
    
    /** Handler for main thread operations. */
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
    
    /** MIDI manager instance. */
    private var midiManager: MidiManager? = null
    /** Currently connected MIDI device. */
    private var midiDevice: MidiDevice? = null
    /** Input port for sending MIDI data. */
    private var inputPort: MidiInputPort? = null
    /** Flag indicating if the sender thread is running. */
    private var isRunning = false
    /** Thread for sending MIDI messages. */
    private var senderThread: Thread? = null
    
    /** Listener for connection status updates. */
    private var statusListener: MidiConnectionListener? = null
    
    /**
     * Sets the status listener for MIDI connection events.
     * @param listener The listener to receive connection status updates.
     */
    fun setStatusListener(listener: MidiConnectionListener?) {
        this.statusListener = listener
    }
    
    /**
     * Initializes the MIDI connection and checks for device availability.
     * @return true if initialization was successful, false otherwise.
     */
    fun initializeMidi(): Boolean {
        // Check MIDI feature support
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            notifyError("This device does not support MIDI")
            return false
        }
        
        midiManager = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
        if (midiManager == null) {
            notifyError("Unable to get MIDI service")
            return false
        }
        
        // Get available MIDI devices
        val devices = midiManager!!.devices
        
        if (devices.isEmpty()) {
            Log.i(TAG, "No MIDI devices currently available, will wait for device connection")
            setupDeviceCallback()
            return true
        }
        
        // Try to open the first available MIDI device
        //openMidiDevice(devices[0])
        // Actually some devices have some weird builtin MIDI port.
        // Make sure to open only USB MIDI devices.
        devices.firstOrNull { it.type == MidiDeviceInfo.TYPE_USB }?.let { device ->
            try {
                openMidiDevice(device)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening MIDI device: ", e)
            }
        }
        
        return true
    }
    
    /**
     * Sets up the device connection callback to handle device addition and removal.
     */
    private fun setupDeviceCallback() {
        midiManager?.registerDeviceCallback(object : MidiManager.DeviceCallback() {
            override fun onDeviceAdded(device: MidiDeviceInfo) {
                Log.i(TAG, "MIDI device connected: ${getDeviceName(device)}")
                if (midiDevice == null) {
                    openMidiDevice(device)
                }
            }
            
            override fun onDeviceRemoved(device: MidiDeviceInfo) {
                Log.i(TAG, "MIDI device disconnected: ${getDeviceName(device)}")
                closeMidiDevice()
                notifyDisconnected()
            }
        }, mainHandler)
    }
    
    /**
     * Opens the specified MIDI device and establishes an input port connection.
     * @param deviceInfo The MIDI device information to open.
     */
    private fun openMidiDevice(deviceInfo: MidiDeviceInfo) {
        midiManager?.openDevice(deviceInfo, { device ->
            device?.let {
                midiDevice = it
                
                // Get input port (output from Android perspective, sending data)
                it.info.ports.firstOrNull { port ->
                    port.type == MidiDeviceInfo.PortInfo.TYPE_INPUT
                }?.let { port ->
                    inputPort = it.openInputPort(port.portNumber)?.also {
                        Log.i(TAG, "MIDI input port opened: ${port.portNumber}")
                        notifyConnected(getDeviceName(device.info))
                    }
                }
                
                if (inputPort == null) {
                    Log.e(TAG, "Device has no available input port")
                    notifyError("MIDI device has no available input port")
                }
            } ?: run {
                Log.e(TAG, "Cannot open MIDI device")
                notifyError("Cannot open MIDI device")
            }
        }, mainHandler)
    }
    
    /**
     * Closes the currently connected MIDI device and input port.
     */
    private fun closeMidiDevice() {
        inputPort?.let {
            try {
                it.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing input port", e)
            }
            inputPort = null
        }
        
        midiDevice?.let {
            try {
                it.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing MIDI device", e)
            }
            midiDevice = null
        }
    }
    
    /**
     * Starts the MIDI sending thread.
     */
    fun start() {
        if (!isRunning) {
            isRunning = true
            senderThread = Thread(this, "MidiSender")
            senderThread?.start()
            Log.i(TAG, "MIDI sender thread started")
        }
    }
    
    /**
     * Stops the MIDI sending thread and closes the device connection.
     */
    fun stop() {
        if (isRunning) {
            isRunning = false
            queue.offer(MidiEvent(MidiEvent.Type.TYPE_DISCONNECT))
            
            senderThread?.let {
                try {
                    it.join(1000)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Interrupted while waiting for thread to finish", e)
                }
            }
            
            closeMidiDevice()
            Log.i(TAG, "MIDI sender thread stopped")
        }
    }
    
    /**
     * The main run method for the MIDI sender thread.
     * Processes events from the queue and sends them via MIDI.
     */
    override fun run() {
        Log.i(TAG, "MIDI sender thread running")
        
        while (isRunning) {
            try {
                val event = queue.take()
                
                // Graceful exit
                if (event.type == MidiEvent.Type.TYPE_DISCONNECT) {
                    break
                }
                
                // If no MIDI device connected, skip
                if (inputPort == null) {
                    continue
                }
                
                // Convert event to MIDI messages and send
                val midiMessages = event.toMidiMessages()
                if (midiMessages != null) {
                    sendMidiMessages(midiMessages)
                }
                
            } catch (e: InterruptedException) {
                Log.e(TAG, "Event queue interrupted", e)
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error sending MIDI message", e)
            }
        }
        
        Log.i(TAG, "MIDI sender thread exited")
    }
    
    /**
     * Sends a sequence of MIDI messages through the input port.
     * @param messages The array of MIDI messages to send.
     */
    private fun sendMidiMessages(messages: Array<ByteArray>) {
        if (inputPort == null) {
            return
        }
        
        try {
            for (message in messages) {
                inputPort!!.send(message, 0, message.size)
                
                // Small delay to ensure message order (optional, adjust based on testing)
                // Thread.sleep(0, 100) // 100 nanoseconds
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error sending MIDI data", e)
            notifyError("MIDI send error: ${e.message}")
        }
    }
    
    /**
     * Retrieves the name of the MIDI device from its properties.
     * @param info The MIDI device information.
     * @return The device name or a default name if not available.
     */
    private fun getDeviceName(info: MidiDeviceInfo): String {
        return info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            ?: "Unknown MIDI device"
    }
    
    /**
     * Notifies the listener that a MIDI connection has been established.
     * @param deviceName The name of the connected device.
     */
    private fun notifyConnected(deviceName: String) {
        mainHandler.post {
            statusListener?.onMidiConnected(deviceName)
        }
    }
    
    /**
     * Notifies the listener that the MIDI connection has been lost.
     */
    private fun notifyDisconnected() {
        mainHandler.post {
            statusListener?.onMidiDisconnected()
        }
    }
    
    /**
     * Notifies the listener of a MIDI error.
     * @param error The error message.
     */
    private fun notifyError(error: String) {
        mainHandler.post {
            statusListener?.onMidiError(error)
        }
    }
    
    /**
     * Checks if a MIDI device is currently connected.
     * @return true if connected, false otherwise.
     */
    fun isConnected(): Boolean {
        return inputPort != null
    }
}
