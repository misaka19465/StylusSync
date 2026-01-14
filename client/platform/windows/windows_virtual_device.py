"""
Windows virtual device implementation using VHF KMDF driver

Not Implemented yet.
"""

from ...utils.data import TouchData, ButtonData
from ..virtual_device import VirtualDevice

class WindowsVirtualDevice(VirtualDevice):
    def __init__(self):
        print("Windows virtual device is not implemented yet.")
        exit(-1)

    def emit_touch(self, data: TouchData):
        # Not implemented yet
        pass

    def emit_button(self, data: ButtonData):
        # Not implemented yet
        pass

    def close(self):
        pass
