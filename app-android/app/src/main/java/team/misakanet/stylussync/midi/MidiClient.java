package team.misakanet.stylussync.midi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import team.misakanet.stylussync.listener.MidiConnectionListener;

/**
 * Handles MIDI communication for sending touch events to a connected computer.
 */
public class MidiClient implements Runnable {
    private static final String TAG = "MidiClient";
    
    /** Queue for MIDI events to be sent. */
    private final LinkedBlockingQueue<MidiEvent> eventQueue = new LinkedBlockingQueue<>();
    /** Application context. */
    private final Context context;
    /** Handler for main thread operations. */
    private final Handler mainHandler;
    
    /** MIDI manager instance. */
    private MidiManager midiManager;
    /** Currently connected MIDI device. */
    private MidiDevice midiDevice;
    /** Input port for sending MIDI data. */
    private MidiInputPort inputPort;
    /** Flag indicating if the sender thread is running. */
    private boolean isRunning = false;
    /** Thread for sending MIDI messages. */
    private Thread senderThread;
    
    /** Listener for connection status updates. */
    private MidiConnectionListener statusListener;
    
    /**
     * Constructs a new MidiClient with the given context.
     * @param context The application context.
     */
    public MidiClient(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Sets the status listener for MIDI connection events.
     * @param listener The listener to receive connection status updates.
     */
    public void setStatusListener(MidiConnectionListener listener) {
        this.statusListener = listener;
    }
    
    /**
     * Gets the event queue for adding MIDI events.
     * @return The LinkedBlockingQueue for MIDI events.
     */
    public LinkedBlockingQueue<MidiEvent> getQueue() {
        return eventQueue;
    }
    
    /**
     * Initializes the MIDI connection and checks for device availability.
     * @return true if initialization was successful, false otherwise.
     */
    public boolean initializeMidi() {
        // Check MIDI feature support
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            notifyError("This device does not support MIDI");
            return false;
        }
        
        midiManager = (MidiManager) context.getSystemService(Context.MIDI_SERVICE);
        if (midiManager == null) {
            notifyError("Unable to get MIDI service");
            return false;
        }
        
        // Get available MIDI devices
        MidiDeviceInfo[] devices = midiManager.getDevices();
        
        if (devices.length == 0) {
            Log.i(TAG, "No MIDI devices currently available, will wait for device connection");
            setupDeviceCallback();
            return true;
        }
        
        // Try to open the first available MIDI device
        //openMidiDevice(devices[0]);
        // Actually some devices have some weird builtin MIDI port.
        // Make sure to open only USB MIDI devices.
        for (MidiDeviceInfo device : devices) {
            if (device.getType() == MidiDeviceInfo.TYPE_USB) {
                try{
                    openMidiDevice(device);
                }
                catch (Exception e) {
                    Log.e(TAG, "Error opening MIDI device: ", e);
                    continue;
                }

                break;
            }
        }
        
        return true;
    }
    
    /**
     * Sets up the device connection callback to handle device addition and removal.
     */
    private void setupDeviceCallback() {
        midiManager.registerDeviceCallback(new MidiManager.DeviceCallback() {
            @Override
            public void onDeviceAdded(MidiDeviceInfo device) {
                Log.i(TAG, "MIDI device connected: " + getDeviceName(device));
                if (midiDevice == null) {
                    openMidiDevice(device);
                }
            }
            
            @Override
            public void onDeviceRemoved(MidiDeviceInfo device) {
                Log.i(TAG, "MIDI device disconnected: " + getDeviceName(device));
                closeMidiDevice();
                notifyDisconnected();
            }
        }, mainHandler);
    }
    
    /**
     * Opens the specified MIDI device and establishes an input port connection.
     * @param deviceInfo The MIDI device information to open.
     */
    private void openMidiDevice(MidiDeviceInfo deviceInfo) {
        midiManager.openDevice(deviceInfo, new MidiManager.OnDeviceOpenedListener() {
            @Override
            public void onDeviceOpened(MidiDevice device) {
                if (device == null) {
                    Log.e(TAG, "Cannot open MIDI device");
                    notifyError("Cannot open MIDI device");
                    return;
                }
                
                midiDevice = device;
                
                // Get input port (output from Android perspective, sending data)
                MidiDeviceInfo.PortInfo[] ports = device.getInfo().getPorts();
                for (MidiDeviceInfo.PortInfo port : ports) {
                    if (port.getType() == MidiDeviceInfo.PortInfo.TYPE_INPUT) {
                        inputPort = device.openInputPort(port.getPortNumber());
                        if (inputPort != null) {
                            Log.i(TAG, "MIDI input port opened: " + port.getPortNumber());
                            notifyConnected(getDeviceName(device.getInfo()));
                            break;
                        }
                    }
                }
                
                if (inputPort == null) {
                    Log.e(TAG, "Device has no available input port");
                    notifyError("MIDI device has no available input port");
                }
            }
        }, mainHandler);
    }
    
    /**
     * Closes the currently connected MIDI device and input port.
     */
    private void closeMidiDevice() {
        if (inputPort != null) {
            try {
                inputPort.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing input port", e);
            }
            inputPort = null;
        }
        
        if (midiDevice != null) {
            try {
                midiDevice.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing MIDI device", e);
            }
            midiDevice = null;
        }
    }
    
    /**
     * Starts the MIDI sending thread.
     */
    public void start() {
        if (!isRunning) {
            isRunning = true;
            senderThread = new Thread(this, "MidiSender");
            senderThread.start();
            Log.i(TAG, "MIDI sender thread started");
        }
    }
    
    /**
     * Stops the MIDI sending thread and closes the device connection.
     */
    public void stop() {
        if (isRunning) {
            isRunning = false;
            eventQueue.offer(new MidiEvent(MidiEvent.Type.TYPE_DISCONNECT));
            
            if (senderThread != null) {
                try {
                    senderThread.join(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting for thread to finish", e);
                }
            }
            
            closeMidiDevice();
            Log.i(TAG, "MIDI sender thread stopped");
        }
    }
    
    /**
     * The main run method for the MIDI sender thread.
     * Processes events from the queue and sends them via MIDI.
     */
    @Override
    public void run() {
        Log.i(TAG, "MIDI sender thread running");
        
        while (isRunning) {
            try {
                MidiEvent event = eventQueue.take();
                
                // Graceful exit
                if (event.type == MidiEvent.Type.TYPE_DISCONNECT) {
                    break;
                }
                
                // If no MIDI device connected, skip
                if (inputPort == null) {
                    continue;
                }
                
                // Convert event to MIDI messages and send
                byte[][] midiMessages = event.toMidiMessages();
                if (midiMessages != null) {
                    sendMidiMessages(midiMessages);
                }
                
            } catch (InterruptedException e) {
                Log.e(TAG, "Event queue interrupted", e);
                break;
            } catch (Exception e) {
                Log.e(TAG, "Error sending MIDI message", e);
            }
        }
        
        Log.i(TAG, "MIDI sender thread exited");
    }
    
    /**
     * Sends a sequence of MIDI messages through the input port.
     * @param messages The array of MIDI messages to send.
     */
    private void sendMidiMessages(byte[][] messages) {
        if (inputPort == null) {
            return;
        }
        
        try {
            for (byte[] message : messages) {
                inputPort.send(message, 0, message.length);
                
                // Small delay to ensure message order (optional, adjust based on testing)
                // Thread.sleep(0, 100); // 100 nanoseconds
            }
        } catch (IOException e) {
            Log.e(TAG, "Error sending MIDI data", e);
            notifyError("MIDI send error: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves the name of the MIDI device from its properties.
     * @param info The MIDI device information.
     * @return The device name or a default name if not available.
     */
    private String getDeviceName(MidiDeviceInfo info) {
        String name = info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME);
        if (name == null || name.isEmpty()) {
            name = info.getProperties().getString(MidiDeviceInfo.PROPERTY_PRODUCT);
        }
        if (name == null || name.isEmpty()) {
            name = "Unknown MIDI device";
        }
        return name;
    }
    
    /**
     * Notifies the listener that a MIDI connection has been established.
     * @param deviceName The name of the connected device.
     */
    private void notifyConnected(String deviceName) {
        mainHandler.post(() -> {
            if (statusListener != null) {
                statusListener.onMidiConnected(deviceName);
            }
        });
    }
    
    /**
     * Notifies the listener that the MIDI connection has been lost.
     */
    private void notifyDisconnected() {
        mainHandler.post(() -> {
            if (statusListener != null) {
                statusListener.onMidiDisconnected();
            }
        });
    }
    
    /**
     * Notifies the listener of a MIDI error.
     * @param error The error message.
     */
    private void notifyError(String error) {
        mainHandler.post(() -> {
            if (statusListener != null) {
                statusListener.onMidiError(error);
            }
        });
    }
    
    /**
     * Checks if a MIDI device is currently connected.
     * @return true if connected, false otherwise.
     */
    public boolean isConnected() {
        return inputPort != null;
    }
}
