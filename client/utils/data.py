"""
Data classes for StylusSync
"""


class TouchData:
    """Data class for touch/position events"""

    def __init__(self, x: int, y: int, pressure: int, sequence: int):
        self.x = x
        self.y = y
        self.pressure = pressure
        self.sequence = sequence


class ButtonData:
    """Data class for button events"""

    def __init__(
        self,
        button_id: int,
        button_down: bool,
        x: int,
        y: int,
        pressure: int,
        sequence: int,
    ):
        self.button_id = button_id
        self.button_down = button_down
        self.x = x
        self.y = y
        self.pressure = pressure
        self.sequence = sequence
