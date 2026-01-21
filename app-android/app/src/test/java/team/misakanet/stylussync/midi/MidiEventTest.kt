package team.misakanet.stylussync.midi

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for MidiEvent class.
 */
class MidiEventTest {

    @Test
    fun testDecode3x7BitTo16Bit() {
        // Test decoding known values
        val msb = 30 // 7 bits
        val mid = 64 // 7 bits
        val lsb = 16 // 2 bits shifted
        val expected = ((msb and 0x7F) shl 9) or ((mid and 0x7F) shl 2) or ((lsb and 0x7F) shr 5)
        val actual = MidiEvent.decode3x7BitTo16Bit(msb, mid, lsb)
        assertEquals(expected, actual)
    }

    @Test
    fun testToMidiMessages_Motion() {
        // Test motion event
        val event = MidiEvent(MidiEvent.Type.TYPE_MOTION, 1000.toShort(), 2000.toShort(), 3000.toShort())
        val messages = event.toMidiMessages()
        assertNotNull(messages)
        assertEquals(12, messages?.size) // Motion event has 12 messages
    }

    @Test
    fun testToMidiMessages_Button() {
        // Test button event
        val event = MidiEvent(MidiEvent.Type.TYPE_BUTTON, 1000.toShort(), 2000.toShort(), 3000.toShort(), 1, true)
        val messages = event.toMidiMessages()
        assertNotNull(messages)
        assertEquals(14, messages?.size) // Button event has 14 messages
    }

    @Test
    fun testToMidiMessages_Disconnect() {
        // Test disconnect event
        val event = MidiEvent(MidiEvent.Type.TYPE_DISCONNECT)
        val messages = event.toMidiMessages()
        assertNull(messages) // Disconnect returns null
    }

    @Test
    fun testGetSequenceNumber() {
        // Test sequence number getter
        val seq = MidiEvent.getSequenceNumber()
        assertTrue(seq >= 0 && seq <= 127)
    }
}
