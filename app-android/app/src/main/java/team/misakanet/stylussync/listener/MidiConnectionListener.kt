package team.misakanet.stylussync.listener

/**
 * Interface for receiving MIDI connection status callbacks.
 */
interface MidiConnectionListener {
    
    /**
     * Called when a MIDI device is successfully connected.
     * @param deviceName The name of the connected MIDI device.
     */
    fun onMidiConnected(deviceName: String)
    
    /**
     * Called when the MIDI device is disconnected.
     */
    fun onMidiDisconnected()
    
    /**
     * Called when a MIDI error occurs.
     * @param error The error message.
     */
    fun onMidiError(error: String)
}
