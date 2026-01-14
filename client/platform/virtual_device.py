"""
Virtual device classes for StylusSync
"""

from abc import ABC, abstractmethod

from ..utils.data import TouchData, ButtonData


class VirtualDevice(ABC):
    """Abstract base class for virtual input devices"""

    @abstractmethod
    def emit_touch(self, data: TouchData):
        """Emit touch event"""
        pass

    @abstractmethod
    def emit_button(self, data: ButtonData):
        """Emit button event"""
        pass

    @abstractmethod
    def close(self):
        """Close the virtual device"""
        pass
