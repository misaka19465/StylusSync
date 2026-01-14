package team.misakanet.stylussync.midi;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for MidiEvent class.
 */
public class MidiEventTest {

    @Test
    public void testDecode3x7BitTo16Bit() {
        // Test decoding known values
        int msb = 30; // 7 bits
        int mid = 64; // 7 bits
        int lsb = 16; // 2 bits shifted
        int expected = ((msb & 0x7F) << 9) | ((mid & 0x7F) << 2) | ((lsb & 0x7F) >> 5);
        int actual = MidiEvent.decode3x7BitTo16Bit(msb, mid, lsb);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMidiMessages_Motion() {
        // Test motion event
        MidiEvent event = new MidiEvent(MidiEvent.Type.TYPE_MOTION, (short) 1000, (short) 2000, (short) 3000);
        byte[][] messages = event.toMidiMessages();
        assertNotNull(messages);
        assertEquals(12, messages.length); // Motion event has 12 messages
    }

    @Test
    public void testToMidiMessages_Button() {
        // Test button event
        MidiEvent event = new MidiEvent(MidiEvent.Type.TYPE_BUTTON, (short) 1000, (short) 2000, (short) 3000, 1, true);
        byte[][] messages = event.toMidiMessages();
        assertNotNull(messages);
        assertEquals(14, messages.length); // Button event has 14 messages
    }

    @Test
    public void testToMidiMessages_Disconnect() {
        // Test disconnect event
        MidiEvent event = new MidiEvent(MidiEvent.Type.TYPE_DISCONNECT);
        byte[][] messages = event.toMidiMessages();
        assertNull(messages); // Disconnect returns null
    }

    @Test
    public void testGetSequenceNumber() {
        // Test sequence number getter
        byte seq = MidiEvent.getSequenceNumber();
        assertTrue(seq >= 0 && seq <= 127);
    }
}