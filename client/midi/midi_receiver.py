"""
StylusSync MIDI Receiver
Handles MIDI connection and message parsing
"""

import rtmidi
from typing import Callable, Optional


class MidiReceiver:
    """MIDI message receiver and parser"""

    # Controller numbers
    CC_X_MSB = 1
    CC_X_MID = 2
    CC_X_LSB = 3
    CC_Y_MSB = 4
    CC_Y_MID = 5
    CC_Y_LSB = 6
    CC_PRESSURE_MSB = 7
    CC_PRESSURE_MID = 8
    CC_PRESSURE_LSB = 9
    CC_SEQUENCE = 10

    CC_EVENT_TYPE = 20
    CC_BUTTON_ID = 21
    CC_BUTTON_STATE = 22
    CC_DATA_COMPLETE = 30

    # Channels
    CHANNEL_DATA = 0
    CHANNEL_CONTROL = 1

    def __init__(self):
        self.midi_in = rtmidi.MidiIn()
        self.port = None
        self.callback: Optional[Callable] = None

        # Data buffer
        self.x_msb = 0
        self.x_mid = 0
        self.x_lsb = 0
        self.y_msb = 0
        self.y_mid = 0
        self.y_lsb = 0
        self.p_msb = 0
        self.p_mid = 0
        self.p_lsb = 0
        self.sequence = 0

        self.event_type = 0
        self.button_id = 0
        self.button_state = 0

        self.last_sequence = -1

    def set_callback(self, callback: Callable):
        """Set callback function for processed events"""
        self.callback = callback

    def list_ports(self):
        """List available MIDI input ports"""
        ports = self.midi_in.get_ports()

        if not ports:
            print("No available MIDI input ports")
            return []

        print("Available MIDI ports:")
        for i, port in enumerate(ports):
            print(f"  {i}: {port}")

        return ports

    def connect(self, port_index: int = 0):
        """Connect to specified MIDI port"""
        ports = self.midi_in.get_ports()

        if not ports:
            print("Error: No available MIDI ports")
            return False

        if port_index >= len(ports):
            print(f"Error: Port index {port_index} out of range")
            return False

        self.midi_in.open_port(port_index)
        self.port = port_index
        print(f"Connected to port: {ports[port_index]}")

        # Set callback function
        self.midi_in.set_callback(self.on_midi_message)

        return True

    def on_midi_message(self, event, data=None):
        """MIDI message callback function"""
        message, deltatime = event

        if len(message) < 3:
            return

        status = message[0]
        controller = message[1]
        value = message[2]

        # Check if it's a Control Change message
        if (status & 0xF0) != 0xB0:
            return

        channel = status & 0x0F

        # Parse message
        if channel == self.CHANNEL_DATA:
            self.parse_data_message(controller, value)
        elif channel == self.CHANNEL_CONTROL:
            self.parse_control_message(controller, value)

    def parse_data_message(self, controller: int, value: int):
        """Parse data channel message"""
        if controller == self.CC_X_MSB:
            self.x_msb = value
        elif controller == self.CC_X_MID:
            self.x_mid = value
        elif controller == self.CC_X_LSB:
            self.x_lsb = value
        elif controller == self.CC_Y_MSB:
            self.y_msb = value
        elif controller == self.CC_Y_MID:
            self.y_mid = value
        elif controller == self.CC_Y_LSB:
            self.y_lsb = value
        elif controller == self.CC_PRESSURE_MSB:
            self.p_msb = value
        elif controller == self.CC_PRESSURE_MID:
            self.p_mid = value
        elif controller == self.CC_PRESSURE_LSB:
            self.p_lsb = value
        elif controller == self.CC_SEQUENCE:
            self.sequence = value

    def parse_control_message(self, controller: int, value: int):
        """Parse control channel message"""
        if controller == self.CC_EVENT_TYPE:
            self.event_type = value
        elif controller == self.CC_BUTTON_ID:
            self.button_id = value
        elif controller == self.CC_BUTTON_STATE:
            self.button_state = value
        elif controller == self.CC_DATA_COMPLETE:
            if value == 127:
                self.process_complete_event()

    def decode_16bit(self, msb: int, mid: int, lsb: int) -> int:
        """Decode three 7-bit values to 16-bit value"""
        return ((msb & 0x7F) << 9) | ((mid & 0x7F) << 2) | ((lsb & 0x7F) >> 5)

    def process_complete_event(self):
        """Process complete event"""
        # Detect packet loss
        if self.last_sequence != -1:
            expected = (self.last_sequence + 1) & 0x7F
            if self.sequence != expected:
                print(
                    f"Warning: Packet loss detected (expected sequence {expected}, received {self.sequence})"
                )

        self.last_sequence = self.sequence

        # Decode coordinates and pressure
        x = self.decode_16bit(self.x_msb, self.x_mid, self.x_lsb)
        y = self.decode_16bit(self.y_msb, self.y_mid, self.y_lsb)
        pressure = self.decode_16bit(self.p_msb, self.p_mid, self.p_lsb)

        # Map button ID (-1 -> 0, 0 -> 1, 1 -> 2, 2 -> 3)
        button_id = self.button_id - 1
        button_down = self.button_state == 127

        # Print event information
        if self.event_type == 0:  # Motion Event
            print(
                f"Motion: X={x}, Y={y}, P={pressure} [seq={self.sequence}]"
            )
        else:  # Button Event
            state = "DOWN" if button_down else "UP"
            print(
                f"Button: ID={button_id}, State={state}, X={x}, Y={y}, P={pressure} [seq={self.sequence}]"
            )

        # Call callback with event data
        if self.callback:
            if self.event_type == 0:
                from client.utils.data import TouchData

                data = TouchData(x, y, pressure, self.sequence)
                self.callback("touch", data)
            else:
                from client.utils.data import ButtonData

                data = ButtonData(
                    button_id, button_down, x, y, pressure, self.sequence
                )
                self.callback("button", data)

    def close(self):
        """Close MIDI connection"""
        if self.midi_in:
            self.midi_in.close_port()
            del self.midi_in
