package team.misakanet.stylussync.midi;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Integration tests for MidiClient class.
 * These tests require an Android device or emulator with MIDI support.
 */
@RunWith(AndroidJUnit4.class)
public class MidiClientIntegrationTest {

    private Context context;
    private MidiClient midiClient;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        midiClient = new MidiClient(context);
    }

    @Test
    public void testInitializeMidi_NoDevices() {
        // Test initialization when no MIDI devices are available
        boolean result = midiClient.initializeMidi();
        // Should return true even if no devices, as it sets up callback
        assertTrue(result);
    }

    @Test
    public void testStartAndStop() {
        midiClient.initializeMidi();
        midiClient.start();
        // Wait a bit
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }
        midiClient.stop();
        // Verify no exceptions
    }

    @Test
    public void testSendMidiEvent() {
        midiClient.initializeMidi();
        midiClient.start();

        // Send a motion event
        MidiEvent event = new MidiEvent(MidiEvent.Type.TYPE_MOTION, (short) 1000, (short) 2000, (short) 3000);
        midiClient.getQueue().offer(event);

        // Wait for processing
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // Ignore
        }

        midiClient.stop();
        // In a real test, verify MIDI output if possible
    }

    @Test
    public void testIsConnected_NoDevice() {
        midiClient.initializeMidi();
        boolean connected = midiClient.isConnected();
        assertFalse(connected);
    }
}