package team.misakanet.stylussync.midi

import android.util.Log

/**
 * Represents a MIDI event that encapsulates touch event data and converts it to MIDI messages.
 */
class MidiEvent {
    
    companion object {
        private const val TAG = "MidiEvent"
        
        // MIDI controller number definitions
        /** Controller number for X coordinate MSB. */
        private const val CC_X_MSB = 1
        /** Controller number for X coordinate MID. */
        private const val CC_X_MID = 2
        /** Controller number for X coordinate LSB. */
        private const val CC_X_LSB = 3
        /** Controller number for Y coordinate MSB. */
        private const val CC_Y_MSB = 4
        /** Controller number for Y coordinate MID. */
        private const val CC_Y_MID = 5
        /** Controller number for Y coordinate LSB. */
        private const val CC_Y_LSB = 6
        /** Controller number for pressure MSB. */
        private const val CC_PRESSURE_MSB = 7
        /** Controller number for pressure MID. */
        private const val CC_PRESSURE_MID = 8
        /** Controller number for pressure LSB. */
        private const val CC_PRESSURE_LSB = 9
        /** Controller number for sequence. */
        private const val CC_SEQUENCE = 10
        
        /** Controller number for event type. */
        private const val CC_EVENT_TYPE = 20
        /** Controller number for button ID. */
        private const val CC_BUTTON_ID = 21
        /** Controller number for button state. */
        private const val CC_BUTTON_STATE = 22
        /** Controller number for data complete flag. */
        private const val CC_DATA_COMPLETE = 30
        
        // MIDI channels
        /** MIDI channel for data transmission. */
        private const val CHANNEL_DATA = 0      // Channel 1 (0-indexed)
        /** MIDI channel for control messages. */
        private const val CHANNEL_CONTROL = 1   // Channel 2 (0-indexed)
        
        /** Sequence number for MIDI messages. */
        private var sequenceNumber: Byte = 0
        
        /**
         * Decodes three 7-bit MIDI values back to a 16-bit value.
         * @param msb The high 7 bits.
         * @param mid The mid 7 bits.
         * @param lsb The low 2 bits (already left-shifted 5 bits).
         * @return The decoded 16-bit value.
         */
        @JvmStatic
        fun decode3x7BitTo16Bit(msb: Int, mid: Int, lsb: Int): Int {
            return ((msb and 0x7F) shl 9) or
                   ((mid and 0x7F) shl 2) or
                   ((lsb and 0x7F) shr 5)
        }
        
        /**
         * Gets the current sequence number for debugging purposes.
         * @return The current sequence number.
         */
        @JvmStatic
        fun getSequenceNumber(): Byte {
            return sequenceNumber
        }
    }
    
    /**
     * Enumeration of MIDI event types.
     */
    enum class Type {
        TYPE_MOTION,    // Hover event
        TYPE_BUTTON,    // Button/touch event
        TYPE_DISCONNECT // Disconnect (internal use)
    }
    
    /** The type of this MIDI event. */
    val type: Type
    /** X coordinate of the touch event. */
    var x: Short = 0
    var y: Short = 0
    var pressure: Short = 0
    /** Button ID and state. */
    var button: Byte = 0
    var buttonDown: Byte = 0
    
    /**
     * Constructs a MidiEvent with the specified type.
     * @param type The type of the MIDI event.
     */
    constructor(type: Type) {
        this.type = type
    }
    
    /**
     * Constructs a MidiEvent with motion data.
     * @param type The type of the MIDI event.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param pressure The pressure value.
     */
    constructor(type: Type, x: Short, y: Short, pressure: Short) : this(type) {
        this.x = x
        this.y = y
        this.pressure = pressure
    }
    
    /**
     * Constructs a MidiEvent with button data.
     * @param type The type of the MIDI event.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param pressure The pressure value.
     * @param button The button ID.
     * @param buttonDown The button state.
     */
    constructor(type: Type, x: Short, y: Short, pressure: Short, button: Int, buttonDown: Boolean) : this(type, x, y, pressure) {
        this.button = button.toByte()
        this.buttonDown = if (buttonDown) 1 else 0
    }
    
    /**
     * Converts the event to a sequence of MIDI messages.
     * @return An array of MIDI messages, each message is 3 bytes [status, controller, value].
     *         Returns null for disconnect events.
     */
    fun toMidiMessages(): Array<ByteArray>? {
        if (type == Type.TYPE_DISCONNECT) {
            return null
        }
        
        // Encode coordinate and pressure values
        val xEncoded = encode16BitTo3x7Bit(x.toInt())
        val yEncoded = encode16BitTo3x7Bit(y.toInt())
        val pEncoded = encode16BitTo3x7Bit(pressure.toInt())
        
        // Increment sequence number
        sequenceNumber = ((sequenceNumber + 1) and 0x7F).toByte()
        
        val messages: Array<ByteArray>
        
        if (type == Type.TYPE_MOTION) {
            // Motion Event: 12 messages
            messages = Array(12) { ByteArray(3) }
            var idx = 0
            
            // 1. Set event type
            messages[idx++] = createControlChange(CHANNEL_CONTROL, CC_EVENT_TYPE, 0)
            
            // 2-4. X coordinate
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_X_MSB, xEncoded[0])
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_X_MID, xEncoded[1])
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_X_LSB, xEncoded[2])
            
            // 5-7. Y coordinate
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_Y_MSB, yEncoded[0])
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_Y_MID, yEncoded[1])
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_Y_LSB, yEncoded[2])
            
            // 8-10. Pressure
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_PRESSURE_MSB, pEncoded[0])
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_PRESSURE_MID, pEncoded[1])
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_PRESSURE_LSB, pEncoded[2])
            
            // 11. Sequence number
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_SEQUENCE, sequenceNumber.toInt())
            
            // 12. Data complete flag
            messages[idx++] = createControlChange(CHANNEL_CONTROL, CC_DATA_COMPLETE, 127)
            
        } else { // TYPE_BUTTON
            // Button Event: 14 messages
            messages = Array(14) { ByteArray(3) }
            var idx = 0
            
            // 1. Set event type
            messages[idx++] = createControlChange(CHANNEL_CONTROL, CC_EVENT_TYPE, 1)
            
            // 2-4. X coordinate
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_X_MSB, xEncoded[0])
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_X_MID, xEncoded[1])
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_X_LSB, xEncoded[2])
            
            // 5-7. Y coordinate
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_Y_MSB, yEncoded[0])
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_Y_MID, yEncoded[1])
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_Y_LSB, yEncoded[2])
            
            // 8-10. Pressure
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_PRESSURE_MSB, pEncoded[0])
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_PRESSURE_MID, pEncoded[1])
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_PRESSURE_LSB, pEncoded[2])
            
            // 11. Button ID (mapping: -1->0, 0->1, 1->2, 2->3)
            val mappedButtonId = button + 1
            messages[idx++] = createControlChange(CHANNEL_CONTROL, CC_BUTTON_ID, mappedButtonId)
            
            // 12. Button state (0->0, 1->127)
            val buttonState = if (buttonDown.toInt() == 1) 127 else 0
            messages[idx++] = createControlChange(CHANNEL_CONTROL, CC_BUTTON_STATE, buttonState)
            
            // 13. Sequence number
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_SEQUENCE, sequenceNumber.toInt())
            
            // 14. Data complete flag
            messages[idx++] = createControlChange(CHANNEL_CONTROL, CC_DATA_COMPLETE, 127)
        }
        
        return messages
    }
    
    /**
     * Encodes a 16-bit value into three 7-bit MIDI values.
     * @param value The 16-bit input value (0-65535).
     * @return An array of three integers [MSB, MID, LSB].
     */
    private fun encode16BitTo3x7Bit(value: Int): IntArray {
        val value16 = value and 0xFFFF // Ensure 16-bit
        
        val msb = (value16 shr 9) and 0x7F  // bits 15-9
        val mid = (value16 shr 2) and 0x7F  // bits 8-2
        val lsb = (value16 shl 5) and 0x7F  // bits 1-0, left-shifted 5 bits
        
        return intArrayOf(msb, mid, lsb)
    }
    
    /**
     * Creates a MIDI Control Change message.
     * @param channel The MIDI channel (0-15).
     * @param controller The controller number (0-127).
     * @param value The controller value (0-127).
     * @return A 3-byte MIDI message [status, controller, value].
     */
    private fun createControlChange(channel: Int, controller: Int, value: Int): ByteArray {
        val message = ByteArray(3)
        message[0] = (0xB0 or (channel and 0x0F)).toByte()  // Control Change + channel
        message[1] = (controller and 0x7F).toByte()         // Controller number
        message[2] = (value and 0x7F).toByte()              // Value
        return message
    }
}
