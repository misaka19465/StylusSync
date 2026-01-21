package team.misakanet.stylussync.midi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration tests for MidiClient class.
 * These tests require an Android device or emulator with MIDI support.
 */
@RunWith(AndroidJUnit4::class)
class MidiClientIntegrationTest {

    private lateinit var context: Context
    private lateinit var midiClient: MidiClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        midiClient = MidiClient(context)
    }

    @Test
    fun testInitializeMidi_NoDevices() {
        // Test initialization when no MIDI devices are available
        val result = midiClient.initializeMidi()
        // Should return true even if no devices, as it sets up callback
        assertTrue(result)
    }

    @Test
    fun testStartAndStop() {
        midiClient.initializeMidi()
        midiClient.start()
        // Wait a bit
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            // Ignore
        }
        midiClient.stop()
        // Verify no exceptions
    }

    @Test
    fun testSendMidiEvent() {
        midiClient.initializeMidi()
        midiClient.start()

        // Send a motion event
        val event = MidiEvent(MidiEvent.Type.TYPE_MOTION, 1000.toShort(), 2000.toShort(), 3000.toShort())
        midiClient.queue.offer(event)

        // Wait for processing
        try {
            Thread.sleep(200)
        } catch (e: InterruptedException) {
            // Ignore
        }

        midiClient.stop()
        // In a real test, verify MIDI output if possible
    }

    @Test
    fun testIsConnected_NoDevice() {
        midiClient.initializeMidi()
        val connected = midiClient.isConnected()
        assertFalse(connected)
    }
}
