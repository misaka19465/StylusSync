package team.misakanet.stylussync.midi;

import android.util.Log;

/**
 * Represents a MIDI event that encapsulates touch event data and converts it to MIDI messages.
 */
public class MidiEvent {
    private static final String TAG = "MidiEvent";
    
    /**
     * Enumeration of MIDI event types.
     */
    public enum Type {
        TYPE_MOTION,    // Hover event
        TYPE_BUTTON,    // Button/touch event
        TYPE_DISCONNECT // Disconnect (internal use)
    }
    
    // MIDI controller number definitions
    /** Controller number for X coordinate MSB. */
    private static final int CC_X_MSB = 1;
    /** Controller number for X coordinate MID. */
    private static final int CC_X_MID = 2;
    /** Controller number for X coordinate LSB. */
    private static final int CC_X_LSB = 3;
    /** Controller number for Y coordinate MSB. */
    private static final int CC_Y_MSB = 4;
    /** Controller number for Y coordinate MID. */
    private static final int CC_Y_MID = 5;
    /** Controller number for Y coordinate LSB. */
    private static final int CC_Y_LSB = 6;
    /** Controller number for pressure MSB. */
    private static final int CC_PRESSURE_MSB = 7;
    /** Controller number for pressure MID. */
    private static final int CC_PRESSURE_MID = 8;
    /** Controller number for pressure LSB. */
    private static final int CC_PRESSURE_LSB = 9;
    /** Controller number for sequence. */
    private static final int CC_SEQUENCE = 10;
    
    /** Controller number for event type. */
    private static final int CC_EVENT_TYPE = 20;
    /** Controller number for button ID. */
    private static final int CC_BUTTON_ID = 21;
    /** Controller number for button state. */
    private static final int CC_BUTTON_STATE = 22;
    /** Controller number for data complete flag. */
    private static final int CC_DATA_COMPLETE = 30;
    
    // MIDI channels
    /** MIDI channel for data transmission. */
    private static final int CHANNEL_DATA = 0;      // Channel 1 (0-indexed)
    /** MIDI channel for control messages. */
    private static final int CHANNEL_CONTROL = 1;   // Channel 2 (0-indexed)
    
    /** The type of this MIDI event. */
    final Type type;
    /** X coordinate of the touch event. */
    short x, y, pressure;
    /** Button ID and state. */
    byte button, button_down;
    
    /** Sequence number for MIDI messages. */
    private static byte sequenceNumber = 0;
    
    /**
     * Constructs a MidiEvent with the specified type.
     * @param type The type of the MIDI event.
     */
    public MidiEvent(Type type) {
        this.type = type;
    }
    
    /**
     * Constructs a MidiEvent with motion data.
     * @param type The type of the MIDI event.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param pressure The pressure value.
     */
    public MidiEvent(Type type, short x, short y, short pressure) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.pressure = pressure;
    }
    
    /**
     * Constructs a MidiEvent with button data.
     * @param type The type of the MIDI event.
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @param pressure The pressure value.
     * @param button The button ID.
     * @param button_down The button state.
     */
    public MidiEvent(Type type, short x, short y, short pressure, int button, boolean button_down) {
        this(type, x, y, pressure);
        this.button = (byte)button;
        this.button_down = (byte)(button_down ? 1 : 0);
    }
    
    /**
     * Converts the event to a sequence of MIDI messages.
     * @return An array of MIDI messages, each message is 3 bytes [status, controller, value].
     *         Returns null for disconnect events.
     */
    public byte[][] toMidiMessages() {
        if (type == Type.TYPE_DISCONNECT) {
            return null;
        }
        
        // Encode coordinate and pressure values
        int[] xEncoded = encode16BitTo3x7Bit(x);
        int[] yEncoded = encode16BitTo3x7Bit(y);
        int[] pEncoded = encode16BitTo3x7Bit(pressure);
        
        // Increment sequence number
        sequenceNumber = (byte)((sequenceNumber + 1) & 0x7F);
        
        byte[][] messages;
        
        if (type == Type.TYPE_MOTION) {
            // Motion Event: 12 messages
            messages = new byte[12][];
            int idx = 0;
            
            // 1. Set event type
            messages[idx++] = createControlChange(CHANNEL_CONTROL, CC_EVENT_TYPE, 0);
            
            // 2-4. X coordinate
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_X_MSB, xEncoded[0]);
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_X_MID, xEncoded[1]);
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_X_LSB, xEncoded[2]);
            
            // 5-7. Y coordinate
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_Y_MSB, yEncoded[0]);
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_Y_MID, yEncoded[1]);
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_Y_LSB, yEncoded[2]);
            
            // 8-10. Pressure
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_PRESSURE_MSB, pEncoded[0]);
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_PRESSURE_MID, pEncoded[1]);
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_PRESSURE_LSB, pEncoded[2]);
            
            // 11. Sequence number
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_SEQUENCE, sequenceNumber);
            
            // 12. Data complete flag
            messages[idx++] = createControlChange(CHANNEL_CONTROL, CC_DATA_COMPLETE, 127);
            
        } else { // TYPE_BUTTON
            // Button Event: 14 messages
            messages = new byte[14][];
            int idx = 0;
            
            // 1. Set event type
            messages[idx++] = createControlChange(CHANNEL_CONTROL, CC_EVENT_TYPE, 1);
            
            // 2-4. X coordinate
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_X_MSB, xEncoded[0]);
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_X_MID, xEncoded[1]);
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_X_LSB, xEncoded[2]);
            
            // 5-7. Y coordinate
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_Y_MSB, yEncoded[0]);
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_Y_MID, yEncoded[1]);
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_Y_LSB, yEncoded[2]);
            
            // 8-10. Pressure
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_PRESSURE_MSB, pEncoded[0]);
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_PRESSURE_MID, pEncoded[1]);
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_PRESSURE_LSB, pEncoded[2]);
            
            // 11. Button ID (mapping: -1->0, 0->1, 1->2, 2->3)
            int mappedButtonId = button + 1;
            messages[idx++] = createControlChange(CHANNEL_CONTROL, CC_BUTTON_ID, mappedButtonId);
            
            // 12. Button state (0->0, 1->127)
            int buttonState = (button_down == 1) ? 127 : 0;
            messages[idx++] = createControlChange(CHANNEL_CONTROL, CC_BUTTON_STATE, buttonState);
            
            // 13. Sequence number
            messages[idx++] = createControlChange(CHANNEL_DATA, CC_SEQUENCE, sequenceNumber);
            
            // 14. Data complete flag
            messages[idx++] = createControlChange(CHANNEL_CONTROL, CC_DATA_COMPLETE, 127);
        }
        
        return messages;
    }
    
    /**
     * Encodes a 16-bit value into three 7-bit MIDI values.
     * @param value The 16-bit input value (0-65535).
     * @return An array of three integers [MSB, MID, LSB].
     */
    private int[] encode16BitTo3x7Bit(int value) {
        value = value & 0xFFFF; // Ensure 16-bit
        
        int msb = (value >> 9) & 0x7F;  // bits 15-9
        int mid = (value >> 2) & 0x7F;  // bits 8-2
        int lsb = (value << 5) & 0x7F;  // bits 1-0, left-shifted 5 bits
        
        return new int[] { msb, mid, lsb };
    }
    
    /**
     * Decodes three 7-bit MIDI values back to a 16-bit value.
     * @param msb The high 7 bits.
     * @param mid The mid 7 bits.
     * @param lsb The low 2 bits (already left-shifted 5 bits).
     * @return The decoded 16-bit value.
     */
    public static int decode3x7BitTo16Bit(int msb, int mid, int lsb) {
        return ((msb & 0x7F) << 9) | 
               ((mid & 0x7F) << 2) | 
               ((lsb & 0x7F) >> 5);
    }
    
    /**
     * Creates a MIDI Control Change message.
     * @param channel The MIDI channel (0-15).
     * @param controller The controller number (0-127).
     * @param value The controller value (0-127).
     * @return A 3-byte MIDI message [status, controller, value].
     */
    private byte[] createControlChange(int channel, int controller, int value) {
        byte[] message = new byte[3];
        message[0] = (byte)(0xB0 | (channel & 0x0F));  // Control Change + channel
        message[1] = (byte)(controller & 0x7F);         // Controller number
        message[2] = (byte)(value & 0x7F);              // Value
        return message;
    }
    
    /**
     * Gets the current sequence number for debugging purposes.
     * @return The current sequence number.
     */
    public static byte getSequenceNumber() {
        return sequenceNumber;
    }
}
