#!/usr/bin/env python3
"""
StylusSync client
Receives MIDI touch data from Android device and converts to input events
"""

import sys
import time
import platform
from typing import Optional

from .midi.midi_receiver import MidiReceiver
from .platform.virtual_device import VirtualDevice


class StylusSyncApp:
    """Main application class"""

    def __init__(self):
        self.midi = MidiReceiver()
        self.device = self._init_device()
        self.midi.set_callback(self.on_event)

    def _init_device(self) -> Optional[VirtualDevice]:
        """Initialize virtual device based on platform"""
        system = platform.system()
        if system == "Linux":
            from client.platform.linux.linux_virtual_device import LinuxVirtualDevice
            return LinuxVirtualDevice()
        elif system == "Windows":
            from client.platform.windows.windows_virtual_device import WindowsVirtualDevice
            return WindowsVirtualDevice()
        else:
            print(f"Warning: Virtual device not implemented for {system}")
            return None

    def on_event(self, event_type: str, data):
        """Handle parsed MIDI events"""
        if not self.device:
            return

        if event_type == "touch":
            self.device.emit_touch(data)
        elif event_type == "button":
            self.device.emit_button(data)

    def run(self):
        """Run the application"""
        print("StylusSync MIDI Receiver v1.0")
        print("=" * 50)

        # List available ports
        ports = self.midi.list_ports()

        if not ports:
            return 1

        # Select port
        port_index = 0
        if len(ports) > 1:
            print("\nPlease select MIDI port (default: 0):")
            try:
                port_index = int(input("> ").strip() or "0")
            except ValueError:
                port_index = 0

        # Connect to port
        if not self.midi.connect(port_index):
            return 1

        # Run receive loop
        print("\nListening for MIDI messages... (Press Ctrl+C to exit)")

        try:
            while True:
                time.sleep(0.1)
        except KeyboardInterrupt:
            print("\nExiting...")

        return 0

    def close(self):
        """Close resources"""
        self.midi.close()
        if self.device:
            self.device.close()


def main():
    """Main function"""
    app = StylusSyncApp()
    try:
        return app.run()
    finally:
        app.close()

if __name__ == "__main__":
    sys.exit(main())
