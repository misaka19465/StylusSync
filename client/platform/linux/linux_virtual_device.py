from ..virtual_device import VirtualDevice
from ...utils.data import TouchData, ButtonData
from typing import Optional

import sys

if sys.platform == "linux":
    from evdev import UInput, ecodes, AbsInfo

    class LinuxVirtualDevice(VirtualDevice):
        """Linux virtual input device using evdev"""

        def __init__(self):
            self.ui: Optional[UInput] = None
            self._init_device()

        def _init_device(self):
            """Initialize Linux virtual input device for stylus events"""
            if UInput is None:
                print("Warning: python-evdev not available; virtual input disabled")
                return

            try:
                capabilities = {
                    ecodes.EV_KEY: [
                        ecodes.BTN_TOOL_PEN,
                        ecodes.BTN_TOUCH,
                        ecodes.BTN_STYLUS,
                        ecodes.BTN_STYLUS2,
                    ],
                    ecodes.EV_ABS: [
                        (
                            ecodes.ABS_X,
                            AbsInfo(
                                value=0,
                                min=0,
                                max=65535,
                                fuzz=0,
                                flat=0,
                                resolution=400,
                            ),
                        ),
                        (
                            ecodes.ABS_Y,
                            AbsInfo(
                                value=0,
                                min=0,
                                max=65535,
                                fuzz=0,
                                flat=0,
                                resolution=400,
                            ),
                        ),
                        (
                            ecodes.ABS_PRESSURE,
                            AbsInfo(
                                value=0, min=0, max=32768, fuzz=0, flat=0, resolution=0
                            ),
                        ),
                    ],
                }

                # bustype 0x06 = virtual
                self.ui = UInput(
                    events=capabilities,
                    name="StylusSync Virtual Stylus",
                    bustype=0x06,
                )
                print("Created virtual input device: StylusSync Virtual Stylus")
            except Exception as e:
                print(f"Warning: Failed to create virtual input device: {e}")
                self.ui = None

        def emit_touch(self, data: TouchData):
            """Emit touch event to virtual device"""
            if not self.ui:
                return

            try:
                # Position and pressure
                self.ui.write(ecodes.EV_ABS, ecodes.ABS_X, data.x)
                self.ui.write(ecodes.EV_ABS, ecodes.ABS_Y, data.y)
                self.ui.write(ecodes.EV_ABS, ecodes.ABS_PRESSURE, data.pressure)

                # Sync report
                self.ui.syn()
            except Exception as e:
                print(f"Warning: Failed to emit touch event: {e}")

        def emit_button(self, data: ButtonData):
            """Emit button event to virtual device"""
            if not self.ui:
                return

            try:
                # Position and pressure
                self.ui.write(ecodes.EV_ABS, ecodes.ABS_X, data.x)
                self.ui.write(ecodes.EV_ABS, ecodes.ABS_Y, data.y)
                self.ui.write(ecodes.EV_ABS, ecodes.ABS_PRESSURE, data.pressure)

                # Button handling, including stylus side buttons
                keycode: Optional[int] = None
                if data.button_id == -1:
                    keycode = ecodes.BTN_TOOL_PEN
                elif data.button_id == 0:
                    keycode = ecodes.BTN_TOUCH
                elif data.button_id == 1:
                    keycode = ecodes.BTN_STYLUS
                elif data.button_id == 2:
                    keycode = ecodes.BTN_STYLUS2

                if keycode is not None:
                    self.ui.write(ecodes.EV_KEY, keycode, 1 if data.button_down else 0)

                # Sync report
                self.ui.syn()
            except Exception as e:
                print(f"Warning: Failed to emit button event: {e}")

        def close(self):
            """Close the virtual device"""
            if self.ui:
                try:
                    self.ui.close()
                except Exception:
                    pass
