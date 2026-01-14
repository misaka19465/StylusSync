package team.misakanet.stylussync.listener;

/**
 * Interface for receiving MIDI connection status callbacks.
 */
public interface MidiConnectionListener {
    
    /**
     * Called when a MIDI device is successfully connected.
     * @param deviceName The name of the connected MIDI device.
     */
    void onMidiConnected(String deviceName);
    
    /**
     * Called when the MIDI device is disconnected.
     */
    void onMidiDisconnected();
    
    /**
     * Called when a MIDI error occurs.
     * @param error The error message.
     */
    void onMidiError(String error);
}
